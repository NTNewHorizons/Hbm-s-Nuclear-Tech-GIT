<#
.SYNOPSIS
Benchmarks the legacy Java 8 build against the modern Java 25 build.

.DESCRIPTION
Creates detached worktrees for two committed revisions, gives each revision an
isolated Gradle cache, runs an untimed warmup, and measures repeated clean
builds. Raw logs, individual timings, and aggregate summaries are written to
build/benchmark-results by default.

.EXAMPLE
.\tools\benchmark-java-migration.ps1 -OldRevision HEAD^ -NewRevision HEAD `
    -Jdk8Home 'C:\Program Files\Eclipse Adoptium\jdk-8.0.504.1-hotspot' `
    -Jdk25Home 'C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot'
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string] $OldRevision,

    [string] $NewRevision = 'HEAD',

    [Parameter(Mandatory = $true)]
    [string] $Jdk8Home,

    [Parameter(Mandatory = $true)]
    [string] $Jdk25Home,

    [ValidateRange(1, 100)]
    [int] $BuildRuns = 7,

    [string] $OutputDirectory,

    [switch] $KeepWorktrees
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Invoke-Git {
    param(
        [Parameter(Mandatory = $true)]
        [string[]] $Arguments,

        [Parameter(Mandatory = $true)]
        [string] $WorkingDirectory
    )

    $result = & git -C $WorkingDirectory @Arguments 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "git $($Arguments -join ' ') failed:`n$($result -join [Environment]::NewLine)"
    }
    return ($result -join [Environment]::NewLine).Trim()
}

function Resolve-JavaHome {
    param([Parameter(Mandatory = $true)][string] $JavaHome)

    $resolvedHome = (Resolve-Path -LiteralPath $JavaHome).Path
    $java = Join-Path $resolvedHome 'bin\java.exe'
    if (-not (Test-Path -LiteralPath $java -PathType Leaf)) {
        throw "No java.exe found under '$resolvedHome'."
    }
    return $resolvedHome
}

function Get-Median {
    param([Parameter(Mandatory = $true)][double[]] $Values)

    $sorted = @($Values | Sort-Object)
    $middle = [int][Math]::Floor($sorted.Count / 2)
    if ($sorted.Count % 2 -eq 0) {
        return ($sorted[$middle - 1] + $sorted[$middle]) / 2
    }
    return $sorted[$middle]
}

function Invoke-GradleBuild {
    param(
        [Parameter(Mandatory = $true)][string] $Label,
        [Parameter(Mandatory = $true)][string] $Worktree,
        [Parameter(Mandatory = $true)][string] $JavaHome,
        [Parameter(Mandatory = $true)][string] $GradleUserHome,
        [Parameter(Mandatory = $true)][string] $LogFile
    )

    $wrapper = Join-Path $Worktree 'gradlew.bat'
    if (-not (Test-Path -LiteralPath $wrapper -PathType Leaf)) {
        throw "Gradle wrapper not found in '$Worktree'."
    }

    $previousJavaHome = $env:JAVA_HOME
    $previousGradleUserHome = $env:GRADLE_USER_HOME
    $env:JAVA_HOME = $JavaHome
    $env:GRADLE_USER_HOME = $GradleUserHome

    try {
        Push-Location $Worktree
        try {
            $timer = [Diagnostics.Stopwatch]::StartNew()
            & $wrapper clean build --no-daemon --no-build-cache --console=plain *> $LogFile
            $exitCode = $LASTEXITCODE
            $timer.Stop()
        } finally {
            Pop-Location
        }
    } finally {
        $env:JAVA_HOME = $previousJavaHome
        $env:GRADLE_USER_HOME = $previousGradleUserHome
    }

    if ($exitCode -ne 0) {
        throw "$Label build failed. See '$LogFile'."
    }

    return [Math]::Round($timer.Elapsed.TotalSeconds, 3)
}

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$null = Invoke-Git -Arguments @('rev-parse', '--show-toplevel') -WorkingDirectory $repositoryRoot
$oldCommit = Invoke-Git -Arguments @('rev-parse', "$OldRevision^{commit}") -WorkingDirectory $repositoryRoot
$newCommit = Invoke-Git -Arguments @('rev-parse', "$NewRevision^{commit}") -WorkingDirectory $repositoryRoot
$workingTreeStatus = Invoke-Git -Arguments @('status', '--porcelain') -WorkingDirectory $repositoryRoot

if (-not [string]::IsNullOrWhiteSpace($workingTreeStatus)) {
    Write-Warning 'Uncommitted changes are not included. NewRevision always refers to a committed Git revision.'
}

if ($oldCommit -eq $newCommit) {
    throw 'OldRevision and NewRevision resolve to the same commit. Commit the migration before benchmarking it.'
}

$jdk8Resolved = Resolve-JavaHome -JavaHome $Jdk8Home
$jdk25Resolved = Resolve-JavaHome -JavaHome $Jdk25Home

if ([string]::IsNullOrWhiteSpace($OutputDirectory)) {
    $stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
    $OutputDirectory = Join-Path $repositoryRoot "build\benchmark-results\$stamp"
}
$OutputDirectory = [IO.Path]::GetFullPath($OutputDirectory)
New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null

$temporaryRoot = Join-Path ([IO.Path]::GetTempPath()) ('hbm-java-benchmark-' + [Guid]::NewGuid().ToString('N'))
$oldWorktree = Join-Path $temporaryRoot 'old'
$newWorktree = Join-Path $temporaryRoot 'new'
$oldGradleHome = Join-Path $temporaryRoot 'gradle-old'
$newGradleHome = Join-Path $temporaryRoot 'gradle-new'
New-Item -ItemType Directory -Path $temporaryRoot -Force | Out-Null

$oldWorktreeCreated = $false
$newWorktreeCreated = $false

try {
    Write-Host 'Creating isolated worktrees...'
    $null = Invoke-Git -Arguments @('worktree', 'add', '--detach', $oldWorktree, $oldCommit) -WorkingDirectory $repositoryRoot
    $oldWorktreeCreated = $true
    $null = Invoke-Git -Arguments @('worktree', 'add', '--detach', $newWorktree, $newCommit) -WorkingDirectory $repositoryRoot
    $newWorktreeCreated = $true

    Write-Host 'Running untimed warmup builds...'
    $null = Invoke-GradleBuild -Label 'old warmup' -Worktree $oldWorktree -JavaHome $jdk8Resolved `
        -GradleUserHome $oldGradleHome -LogFile (Join-Path $OutputDirectory 'old-warmup.log')
    $null = Invoke-GradleBuild -Label 'new warmup' -Worktree $newWorktree -JavaHome $jdk25Resolved `
        -GradleUserHome $newGradleHome -LogFile (Join-Path $OutputDirectory 'new-warmup.log')

    $oldConfiguration = @{
        Label = 'old-java8'
        Tree = $oldWorktree
        Java = $jdk8Resolved
        GradleHome = $oldGradleHome
    }
    $newConfiguration = @{
        Label = 'new-java25'
        Tree = $newWorktree
        Java = $jdk25Resolved
        GradleHome = $newGradleHome
    }

    $buildResults = @()
    for ($run = 1; $run -le $BuildRuns; $run++) {
        # Alternate order to reduce bias from CPU temperature and background work.
        $configurations = if ($run % 2 -eq 0) {
            @($newConfiguration, $oldConfiguration)
        } else {
            @($oldConfiguration, $newConfiguration)
        }

        foreach ($configuration in $configurations) {
            Write-Host "Build benchmark $($configuration.Label), run $run/$BuildRuns..."
            $log = Join-Path $OutputDirectory "$($configuration.Label)-build-$run.log"
            $seconds = Invoke-GradleBuild -Label $configuration.Label -Worktree $configuration.Tree `
                -JavaHome $configuration.Java -GradleUserHome $configuration.GradleHome -LogFile $log
            $buildResults += [pscustomobject]@{
                Configuration = $configuration.Label
                Run = $run
                Seconds = $seconds
                Log = $log
            }
        }
    }

    $buildResults | Export-Csv -LiteralPath (Join-Path $OutputDirectory 'build-results.csv') `
        -NoTypeInformation -Encoding UTF8

    $buildSummary = @($buildResults | Group-Object Configuration | ForEach-Object {
        $times = [double[]]@($_.Group.Seconds)
        [pscustomobject]@{
            Configuration = $_.Name
            Runs = $times.Count
            MedianSeconds = [Math]::Round((Get-Median -Values $times), 3)
            MeanSeconds = [Math]::Round(($times | Measure-Object -Average).Average, 3)
            MinimumSeconds = [Math]::Round(($times | Measure-Object -Minimum).Minimum, 3)
            MaximumSeconds = [Math]::Round(($times | Measure-Object -Maximum).Maximum, 3)
        }
    })

    $buildSummary | Export-Csv -LiteralPath (Join-Path $OutputDirectory 'build-summary.csv') `
        -NoTypeInformation -Encoding UTF8
    $buildSummary | Format-Table -AutoSize

    [pscustomobject]@{
        OldCommit = $oldCommit
        NewCommit = $newCommit
        Jdk8Home = $jdk8Resolved
        Jdk25Home = $jdk25Resolved
        BuildRuns = $BuildRuns
        GradleArguments = 'clean build --no-daemon --no-build-cache --console=plain'
        OutputDirectory = $OutputDirectory
    } | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $OutputDirectory 'benchmark-metadata.json') -Encoding UTF8

    Write-Host "Benchmark complete. Results: $OutputDirectory"
} finally {
    if (-not $KeepWorktrees) {
        if ($oldWorktreeCreated) {
            try {
                $null = Invoke-Git -Arguments @('worktree', 'remove', '--force', $oldWorktree) -WorkingDirectory $repositoryRoot
            } catch {
                Write-Warning $_
            }
        }
        if ($newWorktreeCreated) {
            try {
                $null = Invoke-Git -Arguments @('worktree', 'remove', '--force', $newWorktree) -WorkingDirectory $repositoryRoot
            } catch {
                Write-Warning $_
            }
        }

        $resolvedTempParent = [IO.Path]::GetFullPath([IO.Path]::GetTempPath()).TrimEnd('\')
        $resolvedTemporaryRoot = [IO.Path]::GetFullPath($temporaryRoot).TrimEnd('\')
        if ((Split-Path $resolvedTemporaryRoot -Parent) -eq $resolvedTempParent -and
            (Split-Path $resolvedTemporaryRoot -Leaf) -like 'hbm-java-benchmark-*') {
            Remove-Item -LiteralPath $resolvedTemporaryRoot -Recurse -Force -ErrorAction SilentlyContinue
        } else {
            Write-Warning "Refusing to remove unexpected temporary path '$resolvedTemporaryRoot'."
        }
    } else {
        Write-Host "Worktrees retained under: $temporaryRoot"
    }
}
