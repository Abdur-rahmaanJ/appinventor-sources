// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.client.editor.youngandroid.properties;

import static com.google.appinventor.client.Ode.MESSAGES;

import com.google.appinventor.client.editor.simple.components.MockVisibleComponent;
import com.google.appinventor.client.widgets.properties.AdditionalChoicePropertyEditor;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * Property editor for length properties (i.e. width and height).
 *
 */
public class YoungAndroidLengthPropertyEditor extends AdditionalChoicePropertyEditor {
  public static final String CONST_AUTOMATIC = "" + MockVisibleComponent.LENGTH_PREFERRED;
  public static final String CONST_FILL_PARENT = "" + MockVisibleComponent.LENGTH_FILL_PARENT;

  private static int uniqueIdSeed = 0;

  private final RadioButton automaticRadioButton;
  private final RadioButton fillParentRadioButton;
  private final RadioButton customLengthRadioButton;
  private final TextBox customLengthField;
  private final ListBox customLengthListValues;

  /**
   * Dropdown option values for the customLengthListValues
   */
  private final String PERCENT = "%";
  private final String CH = "ch";
  private final String CM = "cm";
  private final String EM = "em";
  private final String EX = "ex";
  private final String IN = "in";
  private final String MM = "mm";
  private final String PC = "pc";
  private final String PT = "pt";
  private final String PX = "px";
  private final String REM = "rem";
  private final String VH = "vh";
  private final String VMAX = "vmax";
  private final String VMIN = "vmin";
  private final String VW = "vw";
  
  /**
   * Conversation Factors for Absolute Units
   */
  private final double PX2PX = 1.0;
  private final double CM2PX = 37.795275593333;
  private final double MM2PX = 3.7795275593333;
  private final double IN2PX = 96;
  private final double PC2PX = 16;
  private final double PT2PX = 1.333333333333;
  
  public YoungAndroidLengthPropertyEditor() {
    this(true);
  }

  /**
   * Creates a new length property editor.
   *
   * @param includePercent  whether to include percent of screen option
   */
  public YoungAndroidLengthPropertyEditor(boolean includePercent) {
    // The radio button group cannot be shared across all instances, so we append a unique id.
    int uniqueId = ++uniqueIdSeed;
    String radioButtonGroup = "LengthType-" + uniqueId;
    automaticRadioButton = new RadioButton(radioButtonGroup, MESSAGES.automaticCaption());
    fillParentRadioButton = new RadioButton(radioButtonGroup, MESSAGES.fillParentCaption());
    customLengthRadioButton = new RadioButton(radioButtonGroup);
    customLengthField = new TextBox();
    customLengthField.setVisibleLength(4);
    customLengthField.setMaxLength(4);

    customLengthListValues = new ListBox();
    customLengthListValues.addItem(PERCENT);
    customLengthListValues.addItem(CH);
    customLengthListValues.addItem(CM);
    customLengthListValues.addItem(EM);
    customLengthListValues.addItem(EX);
    customLengthListValues.addItem(IN);
    customLengthListValues.addItem(MM);
    customLengthListValues.addItem(PC);
    customLengthListValues.addItem(PT);
    customLengthListValues.addItem(PX);
    customLengthListValues.addItem(REM);
    customLengthListValues.addItem(VH);
    customLengthListValues.addItem(VMAX);
    customLengthListValues.addItem(VMIN);
    customLengthListValues.addItem(VW);
    
    Panel customRow = new HorizontalPanel();
    customRow.add(customLengthRadioButton);
    customRow.add(customLengthField);
    customRow.add(customLengthListValues);
    Label pixels = new Label(MESSAGES.unitsCaption());
    pixels.setStylePrimaryName("ode-UnitsLabel");
    customRow.add(pixels);

    Panel panel = new VerticalPanel();
    panel.add(automaticRadioButton);
    panel.add(fillParentRadioButton);
    panel.add(customRow);

    automaticRadioButton.addValueChangeHandler(new ValueChangeHandler() {
      @Override
      public void onValueChange(ValueChangeEvent event) {
        // Clear the custom and percent length fields.
        customLengthField.setText("");
      }
    });
    fillParentRadioButton.addValueChangeHandler(new ValueChangeHandler() {
      @Override
      public void onValueChange(ValueChangeEvent event) {
        // Clear the custom and percent length fields.
        customLengthField.setText("");
      }
    });
    customLengthField.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        // If the user clicks on the custom length field, but the radio button for a custom length
        // is not checked, check it.
        if (!customLengthRadioButton.isChecked()) {
          customLengthRadioButton.setChecked(true);
        }
      }
    });

    customLengthListValues.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
	// If the user clicks on the custom length list values field, but the radio button for a custom length
	// is not checked, check it.
	if (!customLengthRadioButton.isChecked()) {
	  customLengthRadioButton.setChecked(true);
	}
      }
    });
    
    initAdditionalChoicePanel(panel);
  }

  @Override
  protected void updateValue() {
    super.updateValue();

    String propertyValue = property.getValue();
    if (propertyValue.equals(CONST_AUTOMATIC)) {
      automaticRadioButton.setChecked(true);
    } else if (propertyValue.equals(CONST_FILL_PARENT)) {
      fillParentRadioButton.setChecked(true);
    } else {
      int v = Integer.parseInt(propertyValue);
      if (v <= MockVisibleComponent.LENGTH_PERCENT_TAG) {
        v = (-v) + MockVisibleComponent.LENGTH_PERCENT_TAG;
      } else {
        customLengthRadioButton.setChecked(true);
        customLengthField.setText(propertyValue);
      }
    }
  }

  @Override
  protected String getPropertyValueSummary() {
    String lengthHint = property.getValue();
    if (lengthHint.equals(CONST_AUTOMATIC)) {
      return MESSAGES.automaticCaption();
    } else if (lengthHint.equals(CONST_FILL_PARENT)) {
      return MESSAGES.fillParentCaption();
    } else {
      int v = Integer.parseInt(lengthHint);
      if (v <= MockVisibleComponent.LENGTH_PERCENT_TAG) {
        v = (-v) + MockVisibleComponent.LENGTH_PERCENT_TAG;
        return MESSAGES.percentSummary("" + v);
      } else {
        return MESSAGES.pixelsSummary(lengthHint);
      }
    }
  }
  
  @Override
  protected boolean okAction() {
    if (automaticRadioButton.isChecked()) {
      property.setValue(CONST_AUTOMATIC);
    } else if (fillParentRadioButton.isChecked()) {
      property.setValue(CONST_FILL_PARENT);
    } else if (customLengthRadioButton.isChecked()) {
     
      String text = customLengthField.getText();

      if (customLengthListValues.getSelectedItemText().equals(PX)) { // Custom length in pixels
	return setAbsoluteValue(text, PX2PX);
      } else if (customLengthListValues.getSelectedItemText().equals(CM)) { // Custom length in cm
	return setAbsoluteValue(text, CM2PX);
      } else if (customLengthListValues.getSelectedItemText().equals(MM)) { // Custom length in mm
	return setAbsoluteValue(text, MM2PX);
      } else if (customLengthListValues.getSelectedItemText().equals(IN)) { // Custom length in inches
	return setAbsoluteValue(text, IN2PX);
      } else if (customLengthListValues.getSelectedItemText().equals(PC)) { // Custom length in pc
	return setAbsoluteValue(text, PC2PX);
      } else if (customLengthListValues.getSelectedItemText().equals(PT)) { // Custom length in inches
	return setAbsoluteValue(text, PT2PX);
      } else if (customLengthListValues.getSelectedItemText().equals("%")) { // Percentage relative to the parent 
	return setRelativeValue(text);
      } else if (customLengthListValues.getSelectedItemText().equals(CH)) { // This unit represents the width, or more precisely the advance measure, of the glyph '0' (zero, the Unicode character U+0030) in the element's font.
	//TODO
	return error();
      } else if (customLengthListValues.getSelectedItemText().equals(EM)) { // This unit represents the calculated font-size of the element. If used on the font-size property itself, it represents the inherited font-size of the element.
	//TODO
	return error();
      } else if (customLengthListValues.getSelectedItemText().equals(EX)) { // This unit represents the x-height of the element's font. On fonts with the 'x' letter, this is generally the height of lowercase letters in the font; 1ex ≈ 0.5em in many fonts.
	//TODO
	return error();
      } else if (customLengthListValues.getSelectedItemText().equals(REM)) { // This unit represents the font-size of the root element (e.g. the font-size of the <html> element). When used on the font-size on this root element, it represents its initial value.
	//TODO
	return error();
      } else if (customLengthListValues.getSelectedItemText().equals(VH)) { // 1/100th of the height of the viewport.
	//TODO
	return error();
      } else if (customLengthListValues.getSelectedItemText().equals(VMAX)) { // 1/100th of the maximum value between the height and the width of the viewport.
	//TODO
	return error();
      } else if (customLengthListValues.getSelectedItemText().equals(VMIN)) { // 1/100th of the minimum value between the height and the width of the viewport.
	//TODO
	return error();
      } else if (customLengthListValues.getSelectedItemText().equals(VW)) { // 1/100th of the width of the viewport.
	//TODO
	return error();
      } else {
	return error();
      }      
    } 
    return true;
  }

  private boolean error() {
    Window.alert(MESSAGES.unSupportedWidthInputValue());
    return false;
  }

  private boolean setRelativeValue(String text) {
    boolean success = false;
    try {
	  int v = Integer.parseInt(text);
	  if (v > 0 && v <= 100) {
	    success = true;
	    property.setValue("" + (-v + MockVisibleComponent.LENGTH_PERCENT_TAG));
	  }
	} catch (NumberFormatException e) {
	  // fall through with success == false
	}
	if (!success) {
	  Window.alert(MESSAGES.nonvalidPercentValue());
	  return false;
	}
    return success;
  }

  private boolean setAbsoluteValue(String text, double factor) {
	// Make sure it's a non-negative number. It is important
	// that this check stay within the custom length case because
	// CONST_AUTOMATIC and CONST_FILL_PARENT are deliberately
	// negative.
	boolean success = false;
	int val = 0;
	try {
	  val = Integer.parseInt(text);
	  if ( val >= 0) {
	    success = true;
	  }
	} catch (NumberFormatException e) {
	  // fall through with success == false
	}
	if (!success) {
	  Window.alert(MESSAGES.nonnumericInputError());
	  return false;
	}
	property.setValue(""+ val*factor);
	return success;
  }
}
