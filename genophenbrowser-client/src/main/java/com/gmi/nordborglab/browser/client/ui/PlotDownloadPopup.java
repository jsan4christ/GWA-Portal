package com.gmi.nordborglab.browser.client.ui;

import com.gmi.nordborglab.browser.client.dispatch.command.GetGWASDataAction;
import com.google.common.base.Preconditions;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.query.client.Function;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import org.gwtbootstrap3.client.ui.AnchorButton;
import org.gwtbootstrap3.client.ui.FormGroup;
import org.gwtbootstrap3.client.ui.InlineRadio;

import java.util.LinkedHashSet;

import static com.google.gwt.query.client.GQuery.$;

/**
 * Created by uemit.seren on 2/19/15.
 */
public class PlotDownloadPopup extends Composite {
    interface PlotDownloadPopupUiBinder extends UiBinder<Widget, PlotDownloadPopup> {

    }

    private static PlotDownloadPopupUiBinder ourUiBinder = GWT.create(PlotDownloadPopupUiBinder.class);

    @UiField
    TextBox macTb;
    @UiField
    DivElement upperLimitLb;
    @UiField
    DivElement lowerLimitLb;
    @UiField
    FormGroup chrGroup;
    @UiField
    FormGroup formatGroup;
    @UiField
    SpanElement macLb;
    @UiField
    AnchorButton downloadBtn;
    @UiField
    FormGroup macGroup;
    @UiField
    FlowPanel chrContainer;

    public enum FORMAT {PNG, PDF}

    private GetGWASDataAction.TYPE type;
    private static final int DEFAULT_MAC = 15;
    private final LinkedHashSet<String> chrs;

    private Long id;

    public PlotDownloadPopup(LinkedHashSet<String> chrs) {
        initWidget(ourUiBinder.createAndBindUi(this));
        macTb.getElement().setAttribute("type", "range");
        macTb.getElement().setAttribute("min", "0");
        macTb.setValue(String.valueOf(DEFAULT_MAC));
        macLb.setInnerText(String.valueOf(DEFAULT_MAC));
        $(macTb).on("input change", new Function() {
            @Override
            public boolean f(Event e) {
                updateMac();
                return true;
            }
        });
        this.chrs = chrs;
        chrContainer.clear();
        InlineRadio radio = new InlineRadio("chr", "All");
        radio.setFormValue("");
        radio.setValue(true);
        chrContainer.add(radio);
        for (String chr : chrs) {
            radio = new InlineRadio("chr", chr);
            radio.setFormValue(chr);
            chrContainer.add(radio);
        }
        // initialize checkboxes for chromosome
    }


    public void setSettings(Long id, GetGWASDataAction.TYPE type) {
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(id);
        this.id = id;
        this.type = type;
        updateLink();
    }

    public void setMaxMac(int maxMac) {
        macTb.getElement().setAttribute("max", String.valueOf(maxMac));
        upperLimitLb.setInnerText(String.valueOf(maxMac));
    }


    @UiHandler("macTb")
    public void onChangeMac(ChangeEvent e) {
        updateMac();
    }

    private void updateMac() {
        macLb.setInnerText(macTb.getValue());
        updateLink();
    }


    private int getCurrentMac() {
        int currentMac = DEFAULT_MAC;
        try {
            currentMac = Integer.parseInt(macTb.getValue());
        } catch (NumberFormatException ex) {
        }
        return currentMac;
    }

    private FORMAT getCurrentFormat() {
        return FORMAT.valueOf($(formatGroup).find("input:radio:checked").val());
    }


    private String getCurrentRestEndpoint() {
        switch (type) {
            case STUDY:
                return "analyses";
            case GWASVIEWER:
                return "gwasviewer";
        }
        throw new RuntimeException("Type " + type + " not supported");
    }

    public String getDownloadUrl() {
        if (id == null)
            return "";
        String url = GWT.getHostPageBaseURL() + "/api/" + getCurrentRestEndpoint() + "/" + id + "/plots?mac=" + getCurrentMac();
        FORMAT format = getCurrentFormat();
        if (format != null)
            url += "&format=" + format.name().toLowerCase();
        String chr = getCurrentChr();
        if (chr != null && !chr.isEmpty())
            url += "&chr=" + chr;
        return url;
    }

    private String getCurrentChr() {
        return $(chrGroup).find("input:radio:checked").val();
    }

    private void updateLink() {
        downloadBtn.setHref(getDownloadUrl());
    }

    @Override
    protected void onAttach() {
        super.onAttach();
        // has to be done here otherwise events are not fired
        $("input:radio", getWidget()).change(new Function() {
            @Override
            public boolean f(Event e) {
                updateLink();
                return true;
            }
        });
    }

    @Override
    protected void onDetach() {
        super.onDetach();
        $("input:radio", getWidget()).unbind(Event.ONCHANGE);
    }

    public void setMacFilterEnabled(boolean enable) {
        macTb.setEnabled(enable);
        macGroup.setVisible(enable);
        if (enable) {
            macTb.setValue(String.valueOf(DEFAULT_MAC));
        } else {
            macTb.setValue("0");
        }
        updateMac();
    }


}