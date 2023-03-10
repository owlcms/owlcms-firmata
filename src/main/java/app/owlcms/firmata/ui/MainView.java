package app.owlcms.firmata.ui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.compress.utils.FileNameUtils;
import org.slf4j.LoggerFactory;

import com.fazecast.jSerialComm.SerialPort;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep.LabelsPosition;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.UploadI18N;
import com.vaadin.flow.component.upload.UploadI18N.AddFiles;
import com.vaadin.flow.component.upload.UploadI18N.Uploading;
import com.vaadin.flow.component.upload.receivers.FileBuffer;
import com.vaadin.flow.router.Route;

import app.owlcms.firmata.devicespec.DeviceType;
import app.owlcms.firmata.utils.Config;
import ch.qos.logback.classic.Logger;

/**
 * The main view contains a button and a click listener.
 */
@Route("")
public class MainView extends VerticalLayout {
	FileBuffer fileBuffer = new FileUploader(fn -> Paths.get(fn).toFile());
	FormLayout form = new FormLayout();
	private FirmataService service;
	private UI ui;
	private Logger logger = (Logger) LoggerFactory.getLogger(MainView.class);

	public MainView() {
		setWidth("80%");
		this.getElement().getStyle().set("margin", "1em");
		form.setResponsiveSteps(new ResponsiveStep("0px", 1, LabelsPosition.ASIDE));

		var title = new H2("owlcms Refereeing Device Control");
		title.getStyle().set("margin-top", "0");
		add(title);

		var deviceSelectionTitle = new H3("Device Selection");
		deviceSelectionTitle.getStyle().set("margin-top", "0");
		RadioButtonGroup<DeviceType> blueowlSelector = new RadioButtonGroup<>();
		RadioButtonGroup<DeviceType> customSelector = new RadioButtonGroup<>();
		Upload upload = new Upload(fileBuffer);

		List<DeviceType> values = Arrays.asList(DeviceType.values());
		blueowlSelector.setItems(values.stream().filter(d -> {return d.isBlueOwl;}).collect(Collectors.toList()));
		blueowlSelector.addValueChangeListener(e -> {
			if (e.getValue() == null) {
				return;
			}
			customSelector.clear();
			upload.clearFileList();
			Config.getCurrent().setDevice("blueowl", e.getValue().configName);
		});

		setCustomItems(customSelector);
		customSelector.addValueChangeListener(e -> {
			if (e.getValue() == null) {
				return;
			}
			blueowlSelector.clear();
			upload.clearFileList();
			Config.getCurrent().setDevice("custom", e.getValue().configName);
		});

		UploadI18N i18n = new UploadI18N();
		i18n.setUploading(
				new Uploading().setError(new Uploading.Error().setUnexpectedServerError("File could not be loaded")))
				.setAddFiles(new AddFiles().setOne("Upload Device Configuration"));
		upload.setI18n(i18n);
		upload.addSucceededListener(e -> {
			blueowlSelector.clear();
			setCustomItems(customSelector);
			upload.clearFileList();
		});
		upload.addFailedListener(e -> {
			ConfirmDialog dialog = new ConfirmDialog();
			dialog.setHeader("Upload Failed");
			dialog.setText(new Html("<p>" + e.getReason().getLocalizedMessage() + "</p>"));
			dialog.setConfirmText("OK");
			dialog.open();
			upload.clearFileList();
		});
		form.add(deviceSelectionTitle);
		addFormItemX(blueowlSelector, "Standard Blue-Owl Device");
		addFormItemX(customSelector, "Custom Device");
		addFormItemX(upload, "");

		ComboBox<SerialPort> serialCombo = new ComboBox<>();
		serialCombo.setPlaceholder("Select Port");

		List<SerialPort> serialPorts = getSerialPorts();
		serialCombo.setItems(serialPorts);
		serialCombo.setValue(serialPorts.size() > 0 ? serialPorts.get(0) : null);
		serialCombo.setRequiredIndicatorVisible(true);
		serialCombo.setRequired(isAttached());

		addFormItemX(serialCombo, "Serial Port");
		serialCombo.addThemeName("bordered");
		serialCombo.addValueChangeListener(e -> Config.getCurrent().setSerialPort(e.getValue().getSystemPortName()));

		var mqttConfigTitle = new H3("Server Configuration");

		TextField platformField = new TextField();
		platformField.setValue(Config.getCurrent().getPlatform());
		platformField.addValueChangeListener(e -> Config.getCurrent().setPlatform(e.getValue()));

		TextField mqttServerField = new TextField();
		mqttServerField.setHelperText("This is normally the address of the owlcms server");
		mqttServerField.setValue(Config.getCurrent().getMqttServer());
		mqttServerField.addValueChangeListener(e -> Config.getCurrent().setMqttServer(e.getValue()));

		TextField mqttPortField = new TextField();
		mqttPortField.setValue(Config.getCurrent().getMqttPort());
		mqttPortField.addValueChangeListener(e -> Config.getCurrent().setMqttPort(e.getValue()));

		TextField mqttUsernameField = new TextField();
		mqttUsernameField.setValue(Config.getCurrent().getMqttUsername());
		mqttUsernameField.addValueChangeListener(e -> Config.getCurrent().setMqttUsername(e.getValue()));

		PasswordField mqttPasswordField = new PasswordField();
		mqttPasswordField.setValue(Config.getCurrent().getMqttPassword());
		mqttPasswordField.addValueChangeListener(e -> Config.getCurrent().setMqttPassword(e.getValue()));

		form.add(new Paragraph());
		form.add(mqttConfigTitle);
		addFormItemX(platformField, "Platform");
		addFormItemX(mqttServerField, "MQTT Server");
		addFormItemX(mqttPortField, "MQTT Port");
		addFormItemX(mqttUsernameField, "MQTT Username");
		addFormItemX(mqttPasswordField, "MQTT Password");

		this.add(form);

		Button start = new Button("Start Device", e -> {
			ui = UI.getCurrent();
			updateConfigFromFields(blueowlSelector, customSelector, platformField, serialCombo, mqttServerField,
					mqttPortField, mqttUsernameField, mqttPasswordField);
			String dev = Config.getCurrent().getDevice();
			if (dev != null) {
				service = new FirmataService(() -> confirmOk(), (ex) -> reportError(ex));
				try {
					((FirmataService) service).startDevice();
				} catch (Throwable e1) {
					logger.warn("start button exception {}", e1);
					throw new RuntimeException(e1);
				}
			}
		});
		start.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		start.addClickShortcut(Key.ENTER);
		Button stop = new Button("Stop Device", e -> {
			if (service != null) {
				service.stopDevice();
			}
		});
		var buttons = new HorizontalLayout(start, stop);
		add(buttons);

		// Use custom CSS classes to apply styling. This is defined in
		// shared-styles.css.
		// addClassName("centered-content");

	}

	private void setCustomItems(RadioButtonGroup<DeviceType> customSelector) {
		List<DeviceType> items = computeAvailable(customSelector, DeviceType.values());
		if (items.isEmpty()) {
			customSelector.setErrorMessage("No device definition available. Use the upload dialog to load one.");
			customSelector.setInvalid(true);
		} else {
			customSelector.setInvalid(false);
		}
		customSelector.setItems(items);
	}

	private List<DeviceType> computeAvailable(RadioButtonGroup<DeviceType> customSelector, DeviceType[] values) {
		Path dir = Paths.get(".");
		try {
			return Files.walk(dir, 1).map(f -> FileNameUtils.getBaseName(f)).map(n -> {
				try {
					return DeviceType.valueOf(n);
				} catch (IllegalArgumentException e) {
					return null;
				}
			}).filter(Objects::nonNull).collect(Collectors.toList());
		} catch (IOException e) {
			return List.of();
		}
	}

	private void updateConfigFromFields(RadioButtonGroup<DeviceType> blueowlSelector,
			RadioButtonGroup<DeviceType> customSelector, TextField platformField, ComboBox<SerialPort> serialCombo,
			TextField mqttServerField, TextField mqttPortField, TextField mqttUsernameField,
			PasswordField mqttPasswordField) {
		Config config = Config.getCurrent();
		config.setDevice("nil", null);
		if (blueowlSelector.getValue() != null) {
			config.setDevice("blueowl", blueowlSelector.getValue().configName);
		}
		if (customSelector.getValue() != null) {
			config.setDevice("custom", customSelector.getValue().configName);
		}
		if (config.getDevice() == null) {
			ConfirmDialog dialog = new ConfirmDialog();
			dialog.setHeader("Device Initialization Failed");
			dialog.setText(new Html("<p>Please select a device.</p>"));
			dialog.setConfirmText("OK");
			dialog.open();
			return;
		}
		if (platformField.getValue() != null) {
			config.setPlatform(platformField.getValue());
		}
		if (serialCombo.getValue() != null) {
			config.setSerialPort(serialCombo.getValue().getSystemPortName());
		}
		if (mqttServerField.getValue() != null) {
			config.setMqttServer(mqttServerField.getValue());
		}
		if (mqttPortField.getValue() != null) {
			config.setMqttPort(mqttPortField.getValue());
		}
		if (mqttUsernameField.getValue() != null) {
			config.setMqttUsername(mqttUsernameField.getValue());
		}
		if (mqttPasswordField.getValue() != null) {
			config.setMqttPassword(mqttPasswordField.getValue());
		}
	}

	private void reportError(Throwable ex) {
		logger.error("could not start {}", ex.toString());
		ui.access(() -> {
			ConfirmDialog dialog = new ConfirmDialog();
			dialog.setHeader("Device Initialization Failed");
			dialog.setText(new Html("<p>" + ex.getCause().getMessage().toString() + "</p>"));
			dialog.setConfirmText("OK");
			dialog.open();
		});
	}

	private void confirmOk() {
		ui.access(() -> {
			Notification.show("Device started", 2000, Position.MIDDLE);
		});
	}

	private void addFormItemX(Component c, String string) {
		var item = form.addFormItem(c, string);
		item.getElement().getStyle().set("--vaadin-form-item-label-width", "15em");

	}

	private List<SerialPort> getSerialPorts() {
		SerialPort[] ports = SerialPort.getCommPorts();
		return Arrays.asList(ports);

	}
}
