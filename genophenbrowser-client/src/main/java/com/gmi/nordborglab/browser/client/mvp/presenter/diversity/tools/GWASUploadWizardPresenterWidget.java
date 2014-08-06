package com.gmi.nordborglab.browser.client.mvp.presenter.diversity.tools;

import com.gmi.nordborglab.browser.client.events.GWASUploadedEvent;
import com.gmi.nordborglab.browser.client.events.GoogleAnalyticsEvent;
import com.gmi.nordborglab.browser.client.events.LoadingIndicatorEvent;
import com.gmi.nordborglab.browser.client.mvp.handlers.GWASUploadWizardUiHandlers;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.HasUiHandlers;
import com.gwtplatform.mvp.client.PresenterWidget;
import com.gwtplatform.mvp.client.View;


/**
 * Created with IntelliJ IDEA.
 * User: uemit.seren
 * Date: 2/25/13
 * Time: 4:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class GWASUploadWizardPresenterWidget extends PresenterWidget<GWASUploadWizardPresenterWidget.MyView> implements GWASUploadWizardUiHandlers {


    private boolean multipleUpload = true;
    private String restURL = "provider/gwas/upload";

    public boolean isMultipleUpload() {
        return multipleUpload;
    }

    public interface MyView extends View, HasUiHandlers<GWASUploadWizardUiHandlers> {

        void setmultipleUpload(boolean multipleUpload);

        void setRestURL(String restUrl);
    }

    @Inject
    public GWASUploadWizardPresenterWidget(EventBus eventBus, MyView view) {
        super(eventBus, view);
        getView().setUiHandlers(this);
    }

    @Override
    public void onClose() {
        GWASUploadedEvent.fire(getEventBus(), 0L);
    }

    @Override
    public void onUploadStart() {
        fireEvent(new LoadingIndicatorEvent(true, "Uploading..."));
    }

    @Override
    public void onUploadEnd(double duration) {
        fireEvent(new LoadingIndicatorEvent(false));

        GoogleAnalyticsEvent.fire(getEventBus(), new GoogleAnalyticsEvent.GAEventData("GWAS", "Upload", "URL:" + this.restURL, (int) (duration)));
    }

    @Override
    public void onUploadError(String errorMessage, double duration) {
        fireEvent(new LoadingIndicatorEvent(false));
        GoogleAnalyticsEvent.fire(getEventBus(), new GoogleAnalyticsEvent.GAEventData("GWAS", "Error - Upload", "URL:" + this.restURL + ",Error:" + errorMessage, (int) (duration)));
    }

    public void setMultipleUpload(boolean multipleUpload) {
        this.multipleUpload = multipleUpload;
        getView().setmultipleUpload(multipleUpload);
    }

    public void setRestURL(String restUrl) {
        this.restURL = restUrl;
        getView().setRestURL(restUrl);
    }
}
