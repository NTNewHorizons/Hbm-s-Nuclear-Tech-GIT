package com.hbm.inventory.gui;

import java.awt.Desktop;
import java.io.File;
import java.io.PrintWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.net.URI;
import java.util.Arrays;

import org.lwjgl.Sys;
import org.lwjgl.opengl.GL11;

import com.hbm.lib.RefStrings;
import com.hbm.main.MainRegistry;
import com.hbm.packet.PacketDispatcher;
import com.hbm.packet.toserver.NBTControlPacket;
import com.hbm.tileentity.network.TileEntityRadioAUTOCAL;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import com.hbm.util.i18n.I18nUtil;

public class GUIScreenRadioAUTOCAL extends GuiScreen {

	protected static final ResourceLocation texture = new ResourceLocation(RefStrings.MODID + ":textures/gui/machine/gui_rtty_autocal.png");
	protected TileEntityRadioAUTOCAL autocal;

	protected int xSize = 170;
	protected int ySize = 138;
	protected int guiLeft;
	protected int guiTop;

	public GUIScreenRadioAUTOCAL(TileEntityRadioAUTOCAL autocal) {
		this.autocal = autocal;
	}

	private void browse(URI uri) throws IOException {
		// workaround for Java not supporting all platforms, mostly for Linux
		if (Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
			Desktop.getDesktop().browse(uri);
		} else {
			if (Sys.getVersion().charAt(0) == '3') {
				// probably a LWJGL3ify user, open the folder instead since that somehow seems to work
				File uploadFolder = new File(MainRegistry.configDir.getParentFile(), "hbmComputerUpload");
				if (uploadFolder.exists()) Sys.openURL(uploadFolder.toString());
			} else {
				Sys.openURL(uri.toString());
			}
		}
	}

	@Override
	public void initGui() {
		super.initGui();
		this.guiLeft = (this.width - this.xSize) / 2;
		this.guiTop = (this.height - this.ySize) / 2;
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float f) {
		
		if(this.autocal == null || this.autocal.isInvalid()) {
			Minecraft.getMinecraft().thePlayer.closeScreen();
			return;
		}
		
		this.drawDefaultBackground();
		this.drawGuiContainerBackgroundLayer(f, mouseX, mouseY);
		GL11.glDisable(GL11.GL_LIGHTING);
		this.drawGuiContainerForegroundLayer(mouseX, mouseY);
		GL11.glEnable(GL11.GL_LIGHTING);
	}

	@Override
	protected void mouseClicked(int x, int y, int i) {
		super.mouseClicked(x, y, i);

		NBTTagCompound data = null;

		if(checkClick(x, y, 8, 36, 18, 18)) { data = new NBTTagCompound(); data.setBoolean("on", true); }
		if(checkClick(x, y, 28, 36, 18, 18)) { data = new NBTTagCompound(); data.setBoolean("ignore", true); }
		if(checkClick(x, y, 48, 36, 18, 18)) { data = new NBTTagCompound(); data.setBoolean("auto", true); }

		// open folder and generate new script file
		if(checkClick(x, y, 104, 36, 18, 18)) {
			try {
				File uploadFolder = new File(MainRegistry.configDir.getParentFile(), "hbmComputerUpload");
				File script = new File(uploadFolder, "script.txt");
				if(!uploadFolder.exists()) uploadFolder.mkdir();
				if(!script.exists()) script.createNewFile();
				script.setExecutable(false);
				browse(script.toURI());
			} catch(Throwable ex) { MainRegistry.logger.error("Couldn't open link", ex); }
		}

		// open folder and generate new doc file
		if(checkClick(x, y, 144, 36, 18, 18)) {
			try {
				File uploadFolder = new File(MainRegistry.configDir.getParentFile(), "hbmComputerUpload");
				File doc = new File(uploadFolder, "documentation_v1.1.md");
				if(!uploadFolder.exists()) uploadFolder.mkdir();
				if(!doc.exists()) {
					doc.createNewFile();
					try {
						PrintWriter printer = new PrintWriter(doc, StandardCharsets.US_ASCII.name());
						for(String line : DOCS) printer.println(line);
						printer.close();
					} catch(Throwable e) { }
				}
				browse(doc.toURI());
			} catch(Throwable ex) { MainRegistry.logger.error("Couldn't open link", ex); }
		}

		if(checkClick(x, y, 84, 36, 18, 18)) {
			try {
				File uploadFolder = new File(MainRegistry.configDir.getParentFile(), "hbmComputerUpload");
				File script = new File(uploadFolder, "script.txt");
				if(!uploadFolder.exists()) uploadFolder.mkdir();
				if(!script.exists()) {
					script.createNewFile();
					script.setExecutable(false);
					return;
				}
				
				byte[] bytes = Files.readAllBytes(Paths.get(script.toURI()));
				data = new NBTTagCompound();
				data.setString("payload", new String(bytes, StandardCharsets.UTF_8));

			} catch(Throwable ex) { }
		}

		// this thing can both upload and download files so let's be careful about this
		// the upload is simple, it's just text that is handled by the AUTOCAL, so doing anything malicious isn't more likely than with any other package
		// download is iffy, because we take text from the server, fully user-definable, and save it to disk. it's stored as a txt so accudentally running it
		// or getting it to run itself, should it be a malicious script, is unlikely. still, we want to minimized the chances as much as we can
		// option 1: set file attribute to disallow running (i.e. disable executable perm)
		// option 2: add fluff that would break scripts, however they might work. we can't change the actual lines because we want the script to be edited,
		//           but we can add some extra crap that would either halt common scripting langs entirely or at least disrupt them into not functioning
		// option 3: enforce validation so only MS-ES1 script can be received by the client. this means that info such as comments or incorrectly written commands
		//           are lost, however this is the safest way because it becomes impossible to send malicious code, but it also interferes with regular user operation more

		if(data != null) {
			mc.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F));
			PacketDispatcher.wrapper.sendToServer(new NBTControlPacket(data, autocal.xCoord, autocal.yCoord, autocal.zCoord));
		}
	}

	protected boolean checkClick(int x, int y, int left, int top, int sizeX, int sizeY) {
		return guiLeft + left <= x && guiLeft + left + sizeX > x && guiTop + top < y && guiTop + top + sizeY >= y;
	}

	private void drawGuiContainerForegroundLayer(int x, int y) {

		for(int i = 0; i < autocal.history.length; i++) {
			String line = autocal.history[i];
			if(line == null || line.isEmpty()) continue;
			String localized = I18nUtil.resolveKey(line);
			if(!localized.equals(line)) line = localized;
			this.fontRendererObj.drawString(line, guiLeft + 7, guiTop + 73 + i * 10, 0x00ff00);
		}

		if(checkClick(x, y, 8, 36, 18, 18)) this.func_146283_a(Arrays.asList(new String[] {EnumChatFormatting.RED + I18nUtil.resolveKey("gui.guiscreen_radio_autocal.on_off")}), x, y);
		if(checkClick(x, y, 28, 36, 18, 18)) this.func_146283_a(Arrays.asList(new String[] {EnumChatFormatting.RED + I18nUtil.resolveKey("gui.guiscreen_radio_autocal.ignore_errors"), I18nUtil.resolveKey("gui.guiscreen_radio_autocal.skips_instructions_that_error"), I18nUtil.resolveKey("gui.guiscreen_radio_autocal.leaving_the_computer_turned_on"), I18nUtil.resolveKey("gui.guiscreen_radio_autocal.may_cause_unintended_behavior"), I18nUtil.resolveKey("gui.guiscreen_radio_autocal.and_inconsistencies")}), x, y);
		if(checkClick(x, y, 48, 36, 18, 18)) this.func_146283_a(Arrays.asList(new String[] {EnumChatFormatting.RED + I18nUtil.resolveKey("gui.guiscreen_radio_autocal.automatic_reboot"), I18nUtil.resolveKey("gui.guiscreen_radio_autocal.restarts_the_computer_automatically_when"), I18nUtil.resolveKey("gui.guiscreen_radio_autocal.the_program_stops_due_to_an_error"), I18nUtil.resolveKey("gui.guiscreen_radio_autocal.or_after_finishing")}), x, y);

		if(checkClick(x, y, 84, 36, 18, 18)) this.func_146283_a(Arrays.asList(new String[] {EnumChatFormatting.BLUE + I18nUtil.resolveKey("gui.guiscreen_radio_autocal.upload_program")}), x, y);
		if(checkClick(x, y, 104, 36, 18, 18)) this.func_146283_a(Arrays.asList(new String[] {EnumChatFormatting.BLUE + I18nUtil.resolveKey("gui.guiscreen_radio_autocal.open_program_file")}), x, y);
		if(checkClick(x, y, 124, 36, 18, 18)) this.func_146283_a(Arrays.asList(new String[] {EnumChatFormatting.BLUE + I18nUtil.resolveKey("gui.guiscreen_radio_autocal.download_program"), EnumChatFormatting.RED + I18nUtil.resolveKey("gui.guiscreen_radio_autocal.currently_unsupported")}), x, y);
		if(checkClick(x, y, 144, 36, 18, 18)) this.func_146283_a(Arrays.asList(new String[] {EnumChatFormatting.BLUE + I18nUtil.resolveKey("gui.guiscreen_radio_autocal.open_documentation")}), x, y);
	}

	private void drawGuiContainerBackgroundLayer(float f, int mouseX, int mouseY) {
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

		if(autocal.isOn) drawTexturedModalRect(guiLeft + 8, guiTop + 36, xSize, 0, 18, 18);
		if(!autocal.ignoreError) drawTexturedModalRect(guiLeft + 28, guiTop + 36, xSize, 18, 18, 18);
		if(!autocal.autoReboot) drawTexturedModalRect(guiLeft + 48, guiTop + 36, xSize, 36, 18, 18);
	}

	@Override
	protected void keyTyped(char c, int b) {
		if(b == 1 || b == Minecraft.getMinecraft().gameSettings.keyBindInventory.getKeyCode()) {
			Minecraft.getMinecraft().thePlayer.closeScreen();
		}
	}

	@Override public boolean doesGuiPauseGame() { return false; }


	public static final String[] DOCS = new String[] {
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.autocal_the_automatic_calculator"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.about_this_document"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.this_documentation_is_designed_to_be_understandable_even"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.read_this_document_carefully_as_all_the_described_concep"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.about_autocal"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.the_autocal_automatic_calculator_is_a_basic_machine_that"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.the_first_button_is_the_on_off_switch_turning_the_autoca"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.the_second_button_is_the_ignore_errors_setting_the_red_x"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.the_third_button_is_the_automatic_reboot_setting_if_the"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.the_fourth_button_is_for_uploading_the_script_the_script"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.the_fifth_button_is_for_opening_the_script_file_if_no_sc"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.the_sixth_button_is_for_downloading_a_script_file_the_ex"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.the_simple_workflow_of_programming_an_autocal_unit_is_th"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.about_ms_es1"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.the_script_read_by_autocal_units_is_written_in_machine_s"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.example_ntmserver_set_autocal_max_clock_10_sets_the_max"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.about_ms_es_v1_1"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.ms_es_v1_1_or_ms_es1_feid_first_extended_instruction_set"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.programs_witten_in_ms_es1_are_largely_compatible_with_ms"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.about_the_buffer"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.the_buffer_is_a_single_slot_of_information_that_can_be_u"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.about_the_stack_v1_1"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.the_stack_functions_similar_to_the_buffer_as_in_there_is"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.about_variables"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.ms_es1_allows_named_variables_to_be_saved_for_later_use"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.about_variable_substitution"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.many_commands_allow_for_variable_substitution_i_e_a_spec"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.example_eval_val1_val2_assuming_val1_is_4_and_val2_is_8"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.special_case_the_contents_of_the_buffer_can_also_be_acce"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.about_redstone_over_radio"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.ro_r_has_a_specific_limitation_a_signal_cannot_be_sent_o"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.the_script_commands"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.comments"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.lines_that_start_with_hashtag_space_are_comments_and_the"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.example_this_is_a_comment_a_line_that_does_nothing_but_c"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.nop"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.nop_no_operation_is_an_operation_that_consumes_one_clock"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.clockspeed"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.clockspeed_speed_sets_the_autocal_s_clock_speed_i_e_amou"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.example_clockspeed_5_sets_the_autocal_s_clock_speed_to_f"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.dest"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.dest_name_creates_a_jump_destination_using_the_various_j"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.example_dest_start_creates_a_destination_point_named_sta"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.jmp"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.jmp_destination_will_cause_the_program_to_skip_to_the_de"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.example_jmp_start_jumps_to_the_destination_point_named_s"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.example_jmp_dest_jumps_to_the_destination_point_with_the"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.jmpif"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.jmpif_destination_will_cause_the_program_to_skip_to_the"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.example"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.jmpif_skip"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.nop_2"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.dest_skip"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.nop_2"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.if_the_buffer_is_true_then_the_program_will_jump_to_the"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.jmpnot"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.jmpnot_destination_will_cause_the_program_to_skip_to_the"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.example"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.jmpnot_skip_if"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.nop_then"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.jmp_end"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.dest_skip"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.nop_else"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.dest_end_end"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.by_using_two_jumps_one_conditional_and_one_fixed_we_can"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.endtick"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.endtick_stops_the_script_until_the_next_game_tick_this_i"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.shutdown"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.shutdown_will_turn_the_autocal_unit_off_if_the_autocal_i"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.load"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.load_name_will_take_the_value_of_a_variable_with_the_sup"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.example_load_val_assuming_that_the_variable_val_contains"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.save"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.save_name_will_take_the_value_of_the_buffer_and_save_it"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.example_save_val_assuming_that_the_buffer_contains_the_v"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.buffer"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.buffer_value_will_write_the_supplied_value_directly_to_t"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.example"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.buffer_horseshoe"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.save_item"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.will_buffer_the_value_horseshoe_and_then_save_it_to_the"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.push_v1_1"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.push_value_will_add_the_supplied_value_to_the_stack_if_n"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.pop_v1_1"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.pop_will_remove_the_most_recently_added_value_from_the_s"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.peek_v1_1"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.peek_will_write_the_most_recently_added_value_from_the_s"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.eval"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.eval_statement_will_evaluate_the_supplied_statement_as_a"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.example_eval_4_5_calculates_4_5_and_saves_9_to_the_buffe"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.example_eval_val_2_takes_the_value_of_val_and_divides_it"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.example"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.load_calc"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.eval_2"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.will_write_the_value_of_calc_to_the_buffer_and_then_trea"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.evalr"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.evalr_statement_is_identical_to_eval_however_it_will_rou"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.rounddown_floor"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.rounddown_or_floor_will_try_to_interpret_the_buffer_s_co"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.example_rounddown_assuming_the_buffer_s_value_is_4_2_the"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.example_rounddown_assuming_the_buffer_s_value_is_4_6_the"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.roundup_ceil"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.roundup_or_ceil_will_try_to_interpret_the_buffer_s_conte"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.example_roundup_assuming_the_buffer_s_value_is_4_2_the_b"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.example_roundup_assuming_the_buffer_s_value_is_4_6_the_b"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.round_nearest"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.round_or_nearest_will_try_to_interpret_the_buffer_s_cont"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.example_round_assuming_the_buffer_s_value_is_4_2_the_buf"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.example_round_assuming_the_buffer_s_value_is_4_6_the_buf"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.concat"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.concat_text_works_similarly_to_buffer_text_however_it_ac"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.example_concat_first_and_second_assuming_the_variable_fi"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.eq"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.eq_value_equals_will_try_to_compare_the_buffer_to_the_su"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.example_eq_brick_if_the_buffer_is_brick_then_it_is_set_t"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.example_eq_comp_if_the_buffer_s_value_is_equal_to_the_va"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.gtb"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.gtb_value_greater_than_buffer_will_try_to_compare_the_bu"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.example_gtb_4_if_the_buffer_is_3_or_lower_then_it_is_set"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.example_gtb_comp_if_the_buffer_lower_than_the_value_of_c"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.ltb"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.ltb_value_less_than_buffer_will_try_to_compare_the_buffe"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.example_ltb_4_if_the_buffer_is_5_or_higher_then_it_is_se"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.example_ltb_comp_if_the_buffer_higher_than_the_value_of"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.geb"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.geb_value_greater_than_or_equal_buffer"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.leb"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.leb_value_less_than_or_equal_buffer"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.splitter_v1_1"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.splitter_value_sets_the_splitter_character_or_text_which"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.split_v1_1"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.split_index_will_split_the_buffer_s_contents_grab_the_fr"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.example"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.buffer_cats_and_dogs"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.split_3"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.will_buffer_the_value_of_the_third_fragment_being_dogs"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.splitcount_v1_1"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.splitcount_will_split_the_buffer_s_contents_count_the_nu"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.example"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.buffer_cats_and_dogs_and_birds"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.splitcount"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.will_buffer_the_number_of_fragments_being_5"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.length_v1_1"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.length_will_count_the_number_of_characters_in_the_buffer"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.example"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.buffer_cats_and_dogs_2"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.lenght"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.will_buffer_the_number_of_characters_in_cats_and_dogs_be"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.first_v1_1"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.first_amount_will_grab_the_first_x_characters_from_the_b"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.example"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.buffer_cats_and_dogs_2"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.first_4"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.will_buffer_the_first_four_characters_of_the_buffer_cats"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.last_v1_1"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.last_amount_will_grab_the_last_x_characters_from_the_buf"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.example"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.buffer_cats_and_dogs_2"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.last_4"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.will_buffer_the_last_four_characters_of_the_buffer_dogs"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.send"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.send_channel_will_send_a_redstone_over_radio_signal_over"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.example"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.buffer_hello"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.send_transmission"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.will_send_the_ro_r_signal_hello_on_the_channel_transmiss"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.example"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.buffer_sos"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.send_target"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.will_send_the_ro_r_signal_sos_to_the_channel_saved_in_th"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.listen"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.listen_channel_will_listen_in_on_the_supplied_ro_r_chann"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.example"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.listen_input"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.eval_buffer_100"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.send_output"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.will_take_the_signal_from_the_ro_r_channel_input_multipl"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.poll_v1_1"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.poll_channel_will_listen_to_the_supplied_ro_r_channel_an"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.worldtime_v1_1"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.worldtime_will_save_the_amount_of_ticks_since_world_crea"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.advanced"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.conditional_branches"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.basic_if_else_conditions_are_the_bread_and_butter_of_mos"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.this_example_creates_a_script_that_processed_a_number_va"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.buffer_4_buffer_4_for_comparison"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.gtb_val_is_val_greater_than_our_buffer"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.jmpnot_else_if_not_jump_to_else"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.eval_val_2_if_it_is_divide_by_2"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.save_val_and_save_to_val"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.jmp_end_now_jump_to_end_to_skip_our_else_block"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.dest_else_else"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.eval_val_2_multiply_by_2"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.save_val_and_save_to_val_2"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.dest_end_no_matter_which_branch_we_took_we_always_end_up"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.methods"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.people_who_are_used_to_high_languages_will_already_know"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.this_example_implements_a_basic_lerp_linear_interpolatio"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.dest_lerp"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.eval_a_b_a_i"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.save_result"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.jmp_return"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.this_function_requires_the_variables_a_b_and_i_as_parame"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.buffer_4_first_we_set_up_our_parameters_to_be_used_in_th"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.save_a"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.buffer_7"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.save_b"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.buffer_0_6"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.save_i"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.buffer_returnhere_then_we_define_the_name_of_the_return"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.save_return"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.jmp_lerp_call_the_function"),
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.dest_returnhere_once_the_function_concludes_we_are_back"),
			"",
			I18nUtil.resolveKey("gui.guiscreen_radio_autocal.at_the_end_of_it_all_the_variable_result_now_has_the_des"),
	};
}
