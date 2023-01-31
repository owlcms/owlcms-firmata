package app.owlcms.firmata.ui;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import com.fazecast.jSerialComm.SerialPort;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep.LabelsPosition;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.Route;

import app.owlcms.firmata.devicespec.DeviceType;
import app.owlcms.firmata.utils.Config;

/**
 * The main view contains a button and a click listener.
 */
@Route("")
public class MainView extends VerticalLayout {
	MemoryBuffer memoryBuffer = new MemoryBuffer();
	FormLayout form = new FormLayout();
	private FirmataService service;

	public MainView() {
		setWidth("60%");
		setMargin(true);
		setPadding(true);
		form.setResponsiveSteps(new ResponsiveStep("0px", 1, LabelsPosition.ASIDE));
		
		var title = new H2("owlcms Refereeing Device Control");
		title.getStyle().set("margin-top", "0");
		add(title);
		
		var deviceSelectionTitle = new H4("Device Selection");
		deviceSelectionTitle.getStyle().set("margin-top", "0");
		RadioButtonGroup<DeviceType> deviceSelector = new RadioButtonGroup<>();
		deviceSelector.setItems(DeviceType.values());
		deviceSelector.addValueChangeListener(e -> Config.getCurrent().setDevice("blueowl",e.getValue().configName));
		
		RadioButtonGroup<DeviceType> biyDeviceSelector = new RadioButtonGroup<>();
		biyDeviceSelector.setItems(DeviceType.values());
		biyDeviceSelector.addValueChangeListener(e -> Config.getCurrent().setDevice("biy",e.getValue().configName));
		
		Upload upload = new Upload(memoryBuffer);
		upload.addFinishedListener(e -> {
			InputStream inputStream = memoryBuffer.getInputStream();
			Config.getCurrent().setConfigStream(inputStream);
		});
		form.add(deviceSelectionTitle);
		addFormItemX(deviceSelector, "Standard Blue-Owl Device");
		addFormItemX(biyDeviceSelector, "Default Build-it-yourself Device");
		addFormItemX(upload, "Custom Configuration");
		
		TextField platformField = new TextField();
		platformField.setValue(Config.getCurrent().getPlatform());
		platformField.addValueChangeListener(e -> Config.getCurrent().setPlatform(e.getValue()));
		addFormItemX(platformField, "Platform");
		
		var serialPortTitle = new H4("Serial Port Selection");
		form.add(serialPortTitle);
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
		
		var mqttConfigTitle = new H4("MQTT Server Configuration");
		
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
		
		form.add(mqttConfigTitle);
		addFormItemX(mqttServerField, "MQTT Server");
		addFormItemX(mqttPortField, "MQTT Port");
		addFormItemX(mqttUsernameField, "MQTT Username");
		addFormItemX(mqttPasswordField, "MQTT Password");

		this.add(form);
		
		Button start= new Button("Start Device",
				e -> {
					updateConfigFromFields(deviceSelector, biyDeviceSelector, platformField, serialCombo, mqttServerField, mqttPortField,
							mqttUsernameField, mqttPasswordField);
					service = new FirmataService(() -> confirmOk(), (ex) -> reportError(ex));
					((FirmataService) service).startDevice();
				});
		start.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		start.addClickShortcut(Key.ENTER);
		Button stop = new Button("Stop Device",
				e -> {
					if (service != null) { service.stopDevice(); }
				});
		var buttons = new HorizontalLayout(start, stop);
		add(buttons);
		
		// Use custom CSS classes to apply styling. This is defined in
		// shared-styles.css.
		// addClassName("centered-content");

	}

	private void updateConfigFromFields(RadioButtonGroup<DeviceType> deviceSelector, RadioButtonGroup<DeviceType> biyDeviceSelector,TextField platformField,
			ComboBox<SerialPort> serialCombo, TextField mqttServerField, TextField mqttPortField,
			TextField mqttUsernameField, PasswordField mqttPasswordField) {
		Config config = Config.getCurrent();
		if (deviceSelector.getValue() != null) {
			config.setDevice("blueowl",deviceSelector.getValue().configName);
		}
		if (biyDeviceSelector.getValue() != null) {
			config.setDevice("biy",biyDeviceSelector.getValue().configName);
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
		ConfirmDialog dialog = new ConfirmDialog();
		dialog.setHeader("Device Initialization Failed");
		dialog.setText(new Html("<p>ex.toString()</p>"));
		dialog.setConfirmText("OK");
	}

	private void confirmOk() {
		Notification.show("Device started", 2000, Position.MIDDLE);
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
