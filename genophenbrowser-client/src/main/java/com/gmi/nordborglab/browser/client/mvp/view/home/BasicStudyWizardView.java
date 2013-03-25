package com.gmi.nordborglab.browser.client.mvp.view.home;

import at.gmi.nordborglab.widgets.geochart.client.GeoChart;
import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.TextArea;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.base.IconAnchor;
import com.github.gwtbootstrap.client.ui.constants.ControlGroupType;
import com.gmi.nordborglab.browser.client.events.SelectMethodEvent;
import com.gmi.nordborglab.browser.client.events.SelectTransformationEvent;
import com.gmi.nordborglab.browser.client.mvp.handlers.BasicStudyWizardUiHandlers;
import com.gmi.nordborglab.browser.client.mvp.presenter.home.BasicStudyWizardPresenter;
import com.gmi.nordborglab.browser.client.resources.CardCellListResources;
import com.gmi.nordborglab.browser.client.resources.CustomDataGridResources;
import com.gmi.nordborglab.browser.client.resources.FlagMap;
import com.gmi.nordborglab.browser.client.resources.MainResources;
import com.gmi.nordborglab.browser.client.ui.*;
import com.gmi.nordborglab.browser.client.ui.card.*;
import com.gmi.nordborglab.browser.client.ui.cells.BooleanIconCell;
import com.gmi.nordborglab.browser.client.ui.cells.FlagCell;
import com.gmi.nordborglab.browser.client.util.DataTableUtils;
import com.gmi.nordborglab.browser.client.util.SearchTerm;
import com.gmi.nordborglab.browser.shared.proxy.*;
import com.google.common.collect.*;
import com.google.gwt.cell.client.NumberCell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.*;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.view.client.HasData;
import com.google.gwt.visualization.client.DataTable;
import com.google.gwt.visualization.client.visualizations.MotionChart;
import com.google.gwt.visualization.client.visualizations.corechart.Options;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.SimpleEventBus;
import com.google.web.bindery.requestfactory.gwt.ui.client.EntityProxyKeyProvider;
import com.gwtplatform.mvp.client.ViewWithUiHandlers;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: uemit.seren
 * Date: 1/28/13
 * Time: 7:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class BasicStudyWizardView extends ViewWithUiHandlers<BasicStudyWizardUiHandlers> implements BasicStudyWizardPresenter.MyView {


    public interface Binder extends UiBinder<Widget,BasicStudyWizardView> {}

    private final Widget widget;

    private ExperimentCard selectedExperiment;



    @UiField(provided=true) final MainResources mainRes;
    private final PhenotypeCardCell phenotypeCardCell;
    private SimpleEventBus eventBus = new SimpleEventBus();
    private MethodCard selectedMethod;

    @UiField
    WizardPanel wizard;
    @UiField
    FlowPanel experimentsContainer;

    @UiField
    Button createExperimentBtn;
    @UiField HTMLPanel selectExperimentPanel;
    @UiField
    Modal createExperimentPanel;
    @UiField
    InlineLabel phenotypeCount;
    @UiField LayoutPanel selectPhenotypePanel;

    @UiField SimpleLayoutPanel phenotypeUploadPanel;

    @UiField
    LayoutPanel phenotypeContainterPanel;

    @UiField
    TextBox experimentNameTb;
    @UiField
    TextBox experimentOriginatorTb;
    @UiField
    TextArea experimentDesignTb;

    @UiField
    com.github.gwtbootstrap.client.ui.TextBox phenotypeSearchBox;

    @UiField(provided=true)
    CellList<PhenotypeProxy> phenotypeList;
    @UiField(provided=true) CellList<AlleleAssayProxy> genotypeList;
    @UiField
    TransformationCard noTransformationCard;
    @UiField
    TransformationCard logTransformationCard;
    @UiField
    TransformationCard sqrtTransformationCard;
    @UiField
    TransformationCard boxCoxTransformationCard;
    @UiField
    FlowPanel methodContainer;
    @UiField
    FlowPanel summaryContainer;
    @UiField
    TextBox studyNameTb;
    @UiField
    ControlGroup studyNameGroup;
    @UiField
    SpanElement studyTitleLb;
    @UiField
    NavPills statisticTypePills;
    @UiField
    SimpleLayoutPanel phenotypeChartContainer;
    @UiField
    HTMLPanel phenotypeColumnChartBtnContainer;
    @UiField
    HTMLPanel phenotypeMotionChartBtnContainer;
    @UiField
    HTMLPanel phenotypeGeoChartBtnContainer;
    @UiField(provided = true)
    DataGrid<TraitProxy> missingGenotypesDataGrid;
    @UiField
    CustomPager missingGenotypesPager;

    private TransformationCard selectedTransformationCard;

    private TransformationCard summaryTransformationCard = new TransformationCard();
    private ExperimentCard summaryExperimentCard = new ExperimentCard();
    private GenotypeCard summaryGenotypCard;
    private MethodCard summaryMethodCard = new MethodCard();
    private PhenotypeCard summaryPhenotypeCard;
    private DataTable phenotypeExplorerData;
    private DataTable phenotypeHistogramData;
    private DataTable phenotypeGeoChartData;
    private final FlagMap flagMap;
    private ImmutableBiMap<StatisticTypeProxy,NavLink> statisticTypeLinks;
    private static enum PHENTOYPE_CHART_TYPE {


        HISTOGRAM,GEOCHART,EXPLORER
    }
    private PHENTOYPE_CHART_TYPE activePhenotypeChartType = PHENTOYPE_CHART_TYPE.HISTOGRAM;



    ClickHandler experimentClickHandler = new ClickHandler() {

        @Override
        public void onClick(ClickEvent event) {
            Object source = event.getSource();
            onSelectExperimentCard(source);
        }
    };

    KeyUpHandler experimentKeyUpHandler = new KeyUpHandler() {
        @Override
        public void onKeyUp(KeyUpEvent event) {
            if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
                onSelectExperimentCard(event.getSource());
            }
        }
    };

    ClickHandler statisticTypeClickhandler = new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
            IconAnchor source = (IconAnchor)event.getSource();
            if (source.getParent() instanceof NavLink) {
                NavLink link = (NavLink) source.getParent();
                if (link.isDisabled())
                    return;
                resetStatisticTypeLinkActive();
                link.setActive(true);
                StatisticTypeProxy statisticType = statisticTypeLinks.inverse().get(link);
                getUiHandlers().onSelectStatisticType(statisticType);
            }
        }
    };


    @Inject
    public BasicStudyWizardView(final Binder binder,
                                final PhenotypeCardCell phenotypeCardCell,
                                final GenotypeCardCell genotypeCardCell,
                                final CardCellListResources cardCellListResources,
                                final GenotypeCard genotypeCard,
                                final MainResources mainRes,
                                final CustomDataGridResources dataGridResources,
                                final PhenotypeCard phenotypeCard,
                                final FlagMap flagMap) {
        this.phenotypeCardCell = phenotypeCardCell;
        this.mainRes = mainRes;
        this.flagMap = flagMap;
        this.summaryGenotypCard = genotypeCard;
        this.summaryPhenotypeCard  = phenotypeCard;
        phenotypeList = new CellList<PhenotypeProxy>(phenotypeCardCell,cardCellListResources, new EntityProxyKeyProvider<PhenotypeProxy>());
        genotypeList = new CellList<AlleleAssayProxy>(genotypeCardCell,cardCellListResources,new EntityProxyKeyProvider<AlleleAssayProxy>());
        missingGenotypesDataGrid = new DataGrid<TraitProxy>(50,dataGridResources,new EntityProxyKeyProvider<TraitProxy>(),new HTMLPanel(
                "There are no plants with missing genotypes"));
        widget = binder.createAndBindUi(this);
        missingGenotypesPager.setDisplay(missingGenotypesDataGrid);
        wizard.addCancelButtonClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                getUiHandlers().onCancel();
            }
        });
        wizard.addNextButtonClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                getUiHandlers().onNext();
            }
        });
        wizard.addPreviousButtonClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                getUiHandlers().onPrevious();

            }
        });
        phenotypeContainterPanel.setWidgetVisible(phenotypeUploadPanel, false);
        noTransformationCard.setEventBus(eventBus);
        sqrtTransformationCard.setEventBus(eventBus);
        logTransformationCard.setEventBus(eventBus);
        boxCoxTransformationCard.setEventBus(eventBus);
        SelectTransformationEvent.register(eventBus, new SelectTransformationEvent.Handler() {
            @Override
            public void onSelectTransformation(SelectTransformationEvent event) {
                onSelectTransformationCard(event.getTransformationCard());
            }
        });
        SelectMethodEvent.register(eventBus,new SelectMethodEvent.Handler() {
            @Override
            public void onSelectMethod(SelectMethodEvent event) {
                onSelectMethodCard(event.getCard());
            }
        });
        summaryContainer.add(summaryExperimentCard);
        summaryContainer.add(summaryPhenotypeCard);
        summaryContainer.add(summaryGenotypCard);
        summaryTransformationCard.setChartHeight("125px");
        summaryTransformationCard.setChartWidth("260px");
        summaryContainer.add(summaryTransformationCard);
        summaryContainer.add(summaryMethodCard);

        initMissingGenotypesDataGrid();

    }

    @Override
    public Widget asWidget() {
        return widget;
    }


    @Override
    public void setNextStep() {
        wizard.nextStep();
    }

    @Override
    public void setPreviousStep() {
        wizard.previousStep();
    }

    @Override
    public SearchTerm getPhenotypeSearchTerm() {
        return phenotypeCardCell.getSearchTerm();
    }

    @Override
    public void setExperiments(List<ExperimentProxy> experiments) {
        selectedExperiment = null;
        experimentsContainer.clear();
        for (ExperimentProxy experiment: experiments) {
            ExperimentCard card = new ExperimentCard();
            card.setExperiment(experiment);
            card.addClickHandler(experimentClickHandler);
            card.addKeyUpHandler(experimentKeyUpHandler);
            experimentsContainer.add(card);
        }
    }

    @Override
    public void setMethods(List<StudyProtocolProxy> methods) {
        selectedMethod = null;
        if (methodContainer.getWidgetCount() > 0)
            return;
        for (StudyProtocolProxy studyProtocol:methods) {
            if (studyProtocol == null)
                continue;
            MethodCard card = new MethodCard();
            card.setStudyProtocol(studyProtocol);
            card.setEventBus(eventBus);
            methodContainer.add(card);
        }
    }

    @UiHandler("createExperimentBtn")
    public void onClickCreateExperiment(ClickEvent e) {
        getUiHandlers().onShowCreateExperimentPanel();
    }


    @UiHandler("phenotypeSearchBox")
    public void onKeyUpPhenotypeSearchBox(KeyUpEvent e) {
        getUiHandlers().onSearchPhenotypeName(phenotypeSearchBox.getText());
    }


    @UiHandler("selectPhenotypeBtn")
    public void onClickCreatePhenotype(ClickEvent e) {
        onShowPhenotypeUploadPanel(false);
    }

    @UiHandler("uploadPhenotypeBtn")
    public void onClickUploadPhenotype(ClickEvent e) {
        onShowPhenotypeUploadPanel(true);
    }

    @Override
    public void onShowPhenotypeUploadPanel(boolean isSHow) {
        phenotypeContainterPanel.setWidgetVisible(selectPhenotypePanel,!isSHow);
        phenotypeContainterPanel.setWidgetVisible(phenotypeUploadPanel,isSHow);
    }

    @Override
    public void showTransformationHistogram(TransformationDataProxy.TYPE type,ImmutableSortedMap<Double, Integer> histogram, Double shapiroPval) {
        switch (type) {
            case RAW:
                noTransformationCard.setHistogramData(histogram,shapiroPval);
                onSelectTransformationCard(noTransformationCard);
                break;
            case LOG:
                logTransformationCard.setHistogramData(histogram,shapiroPval);
                break;
            case SQRT:
                sqrtTransformationCard.setHistogramData(histogram,shapiroPval);
                break;

            case BOXCOX:
                boxCoxTransformationCard.setHistogramData(histogram,shapiroPval);
                break;
        }


    }

    private void onSelectTransformationCard(TransformationCard transformationCard) {
        if (selectedTransformationCard == transformationCard)
            return;
        noTransformationCard.setSelected(false);
        logTransformationCard.setSelected(false);
        sqrtTransformationCard.setSelected(false);
        boxCoxTransformationCard.setSelected(false);
        selectedTransformationCard = transformationCard;
        selectedTransformationCard.setSelected(true);
    }



    @Override
    public void setAvailableTransformations(List<TransformationProxy> transformationList) {
        for (TransformationProxy transformation: transformationList) {
            String name = transformation.getName();
            if (name.equalsIgnoreCase("NO")) {
                noTransformationCard.setTransformation(transformation);
            }
            else if (name.equalsIgnoreCase("LOG")) {
                logTransformationCard.setTransformation(transformation);
            }
            else if (name.equalsIgnoreCase("SQRT")) {
                sqrtTransformationCard.setTransformation(transformation);
            }
            else if (name.equalsIgnoreCase("BOXCOX")){
                boxCoxTransformationCard.setTransformation(transformation);
            }
        }
    }

    @Override
    public TransformationProxy getSelectedTransformation() {
        if (selectedTransformationCard != null)
            return selectedTransformationCard.getTransformation();
        return null;
    }

    @UiHandler("saveExperimentBtn")
    public void onClickSaveExperimentBtn(ClickEvent e) {
        getUiHandlers().onSaveExperiment(experimentNameTb.getText(),experimentOriginatorTb.getText(),experimentDesignTb.getText());
    }


    @Override
    public ExperimentProxy getSelectedExperiment() {
        if (!selectExperimentPanel.isVisible())
            return null;
        if (selectedExperiment == null)
            return null;
        return selectedExperiment.getExperiment();
    }



    @Override
    public void showCreateExperimentPanel(boolean show) {
        createExperimentPanel.show();
    }




    @Override
    public boolean isShowUploadPhenotypePanel() {
        return phenotypeUploadPanel.isVisible();
    }


    @Override
    public HasData<AlleleAssayProxy> getGenotypeListDisplay() {
        return genotypeList;
    }

    @Override
    public void setSelectedExperiment(ExperimentProxy experiment) {
        for (int i = 0;i<experimentsContainer.getWidgetCount();i++) {
            Widget widget =experimentsContainer.getWidget(i);
            if (widget instanceof ExperimentCard) {
                ExperimentCard card = (ExperimentCard)widget;
                if (card.getExperiment() == experiment) {
                    selectedExperiment = card;
                    card.setSelected(true);
                    return;
                }
            }
        }
    }

    @Override
    public void hideCreateExperimentPopup() {
        createExperimentPanel.hide();
    }

    @Override
    public HasData<PhenotypeProxy> getPhenotypeListDisplay() {
        return phenotypeList;
    }

    @Override
    public HasText getStudyText() {
        return studyNameTb;
    }

    @Override
    public void validateInputs() {
        if (studyNameTb.getText().equals("")) {
            studyNameGroup.setType(ControlGroupType.ERROR);
        }
        else {
            studyNameGroup.setType(ControlGroupType.SUCCESS);
        }
    }

    @Override
    public void updateSummaryView(AlleleAssayProxy alleleAssay, PhenotypeProxy phenotype) {
        studyTitleLb.setInnerText(studyNameTb.getText());
        summaryExperimentCard.setExperiment(selectedExperiment.getExperiment());
        summaryPhenotypeCard.setValue(phenotype);
        summaryGenotypCard.setValue(alleleAssay);
        summaryTransformationCard.setTransformation(selectedTransformationCard.getTransformation());
        summaryTransformationCard.setHistogramData(selectedTransformationCard.getHistogramData(), selectedTransformationCard.getShapiroScore());
        summaryTransformationCard.onResize();
        summaryMethodCard.setStudyProtocol(selectedMethod.getStudyProtocol());
    }

    @Override
    public void setAvailableStatisticTypes(List<StatisticTypeProxy> statisticTypes)  {
        statisticTypePills.clear();
        ImmutableBiMap.Builder builder = ImmutableBiMap.<StatisticTypeProxy,NavLink>builder();
        for (StatisticTypeProxy statisticType: statisticTypes) {
            if (statisticType == null)
                continue;
            NavLink link = new NavLink(statisticType.getStatType());
            link.setDisabled(true);
            link.addClickHandler(statisticTypeClickhandler);
            builder.put(statisticType,link);
            statisticTypePills.add(link);
        }
        statisticTypeLinks = builder.build();
    }

    @Override
    public StatisticTypeProxy getSelectedStatisticType() {
        StatisticTypeProxy statisticTypeProxy = null;
        for (Map.Entry<StatisticTypeProxy,NavLink> entries: statisticTypeLinks.entrySet()) {
            if (entries.getValue().isActive()) {
                statisticTypeProxy = entries.getKey();
                break;
            }
        }
        return statisticTypeProxy;
    }

    @Override
    public void setAvailableAlleleAssays(final List<AlleleAssayProxy> alleleAssayList) {
         for (final AlleleAssayProxy alleleAssay:alleleAssayList) {
             if (alleleAssay == null)
                 continue;
             missingGenotypesDataGrid.addColumn(new Column<TraitProxy, Boolean>(new BooleanIconCell()) {
                 @Override
                 public Boolean getValue(TraitProxy object) {
                     boolean isFound = false;
                     for (AlleleAssayProxy alleleAssayToCheck:object.getObsUnit().getStock().getPassport().getAlleleAssays()) {
                         if (alleleAssayToCheck.getId() == alleleAssay.getId()) {
                             isFound = true;
                             break;
                         }
                     }
                     return isFound;
                 }
             },alleleAssay.getName());
        }
    }
    @Override
    public HasData<TraitProxy> getMissingGenotypeDisplay() {
        return missingGenotypesDataGrid;
    }

    private void resetStatisticTypeLinks() {
        for (Map.Entry<StatisticTypeProxy,NavLink> entrySet: statisticTypeLinks.entrySet()) {
            NavLink link = entrySet.getValue();
            StatisticTypeProxy statisticType = entrySet.getKey();
            link.setDisabled(true);
            link.setText(statisticType.getStatType());
            link.setActive(false);
        }
    }

    private void resetStatisticTypeLinkActive()  {
       for (NavLink link: statisticTypeLinks.values()) {
           link.setActive(false);
       }
    }

    @Override
    public void setStatisticTypes(List<StatisticTypeProxy> statisticTypes,List<Long> statisticTypeTraitCounts) {
        resetStatisticTypeLinks();
        for (int i =0;i<statisticTypes.size();i++) {
            StatisticTypeProxy statisticType = statisticTypes.get(i);
            NavLink link = statisticTypeLinks.get(statisticType);
            if (link != null) {
                link.setText(statisticType.getStatType()+" ["+statisticTypeTraitCounts.get(i)+"]");
                link.setDisabled(false);
            }
        }
    }

    @Override
    public void setPhenotypeHistogramData(ImmutableSortedMap<Double, Integer> histogram) {
        phenotypeHistogramData = DataTableUtils.createPhenotypeHistogramTable(histogram);
    }

    @Override
    public void setPhenotypExplorerData(ImmutableList<TraitProxy> traitValues) {
        phenotypeExplorerData = DataTableUtils.createPhentoypeExplorerTable(traitValues);
    }

    @Override
    public void setGeoChartData(ImmutableMultiset geoChartData) {
        phenotypeGeoChartData = DataTableUtils.createPhenotypeGeoChartTable(geoChartData);
    }

    @Override
    public void showPhenotypeCharts() {
         drawPhenotypeCharts();
    }

    @Override
    public void resetView() {
        phenotypeHistogramData = DataTableUtils.createPhenotypeHistogramTable(null);
        phenotypeGeoChartData = null;
        phenotypeExplorerData = null;
        selectedExperiment = null;
        selectedTransformationCard = null;
        selectedMethod = null;
        activePhenotypeChartType = PHENTOYPE_CHART_TYPE.HISTOGRAM;
        wizard.reset();
    }

    private void drawPhenotypeCharts() {
        switch (activePhenotypeChartType) {
            case HISTOGRAM:
                if (phenotypeChartContainer.getWidget() == null) {
                    ResizeableColumnChart columnChart = new ResizeableColumnChart(phenotypeHistogramData,createColumnChart());
                    phenotypeChartContainer.add(columnChart);
                }
                else {
                    ResizeableColumnChart columnChart = (ResizeableColumnChart) phenotypeChartContainer.getWidget();
                    columnChart.draw2(phenotypeHistogramData,createColumnChart());
                }
                break;
            case GEOCHART:
                GeoChart geoChart = new GeoChart();
                phenotypeChartContainer.add(geoChart);
                geoChart.draw(phenotypeGeoChartData,createGeoChartOptions());
            case EXPLORER:
                //TODO causes exception
                ResizeableMotionChart motionChart = new ResizeableMotionChart(phenotypeExplorerData,createMotionChartOptions());
                phenotypeChartContainer.add(motionChart);
                break;
        }
    }

    private Options createColumnChart() {
        Options options = DataTableUtils.getDefaultPhenotypeHistogramOptions();
        if (!isStatisticTypeSelected()) {
            options.setTitle("Select a phentoype and a measure type");
            options.setColors("whiteSmoke");
            Options toolTip = Options.create();
            toolTip.set("trigger", "none");
            options.set("tooltip", toolTip);
            Options legendOption = Options.create();
            legendOption.set("position", "none");
            options.set("legend", legendOption);
        }
        return options;
    }

    private boolean isStatisticTypeSelected() {
        boolean selected = false;
        for (int i =0;i<statisticTypePills.getWidgetCount();i++) {
            NavLink link = (NavLink)statisticTypePills.getWidget(i);
            if (link.isActive()) {
                selected = true;
                break;
            }
        }
        return selected;
    }

    private MotionChart.Options createMotionChartOptions() {
        MotionChart.Options options = MotionChart.Options.create();
        options.set(
                "state",
                "%7B%22time%22%3A%22notime%22%2C%22iconType%22%3A%22BUBBLE%22%2C%22xZoomedDataMin%22%3Anull%2C%22yZoomedDataMax%22%3Anull%2C%22xZoomedIn%22%3Afalse%2C%22iconKeySettings%22%3A%5B%5D%2C%22showTrails%22%3Atrue%2C%22xAxisOption%22%3A%222%22%2C%22colorOption%22%3A%224%22%2C%22yAxisOption%22%3A%223%22%2C%22playDuration%22%3A15%2C%22xZoomedDataMax%22%3Anull%2C%22orderedByX%22%3Afalse%2C%22duration%22%3A%7B%22multiplier%22%3A1%2C%22timeUnit%22%3A%22none%22%7D%2C%22xLambda%22%3A1%2C%22orderedByY%22%3Afalse%2C%22sizeOption%22%3A%22_UNISIZE%22%2C%22yZoomedDataMin%22%3Anull%2C%22nonSelectedAlpha%22%3A0.4%2C%22stateVersion%22%3A3%2C%22dimensions%22%3A%7B%22iconDimensions%22%3A%5B%22dim0%22%5D%7D%2C%22yLambda%22%3A1%2C%22yZoomedIn%22%3Afalse%7D%3B");
        options.setHeight(phenotypeChartContainer.getOffsetHeight());
        options.setWidth(phenotypeChartContainer.getOffsetWidth());
        return options;
    }

    private GeoChart.Options createGeoChartOptions() {
        GeoChart.Options options = GeoChart.Options.create();
        options.setTitle("Geographic distribution");
        options.setHeight(phenotypeChartContainer.getOffsetHeight());
        options.setWidth(phenotypeChartContainer.getOffsetWidth());
        return options;
    }

    @Override
    public void setPhenotypeCount(int totalCount, int visibleCount) {
        phenotypeCount.setText(visibleCount + " / " + totalCount);
    }

    private void onSelectExperimentCard(Object source) {
        if (source instanceof FocusPanel) {
            ExperimentCard card = (ExperimentCard) ((FocusPanel) source).getParent();
            if (selectedExperiment != null && selectedExperiment != card) {
                selectedExperiment.setSelected(false);
                getUiHandlers().onSelectedExperimentChanged();
            }
            selectedExperiment = card;
            selectedExperiment.setSelected(true);
        }
    }


    @Override
    public void setInSlot(Object slot, Widget content) {
        if (slot == BasicStudyWizardPresenter.TYPE_SetPhenotypeUploadContent) {
            phenotypeUploadPanel.setWidget(content);
        } else {
            super.setInSlot(slot, content);
        }
    }

    public TransformationCard getSelectedTransformationCard() {
        return selectedTransformationCard;
    }

    private void onSelectMethodCard(MethodCard card) {
        if (selectedMethod != card) {
            selectedMethod = card;
            for (int i = 0;i<methodContainer.getWidgetCount();i++) {
                Widget widget =methodContainer.getWidget(i);
                if (widget instanceof MethodCard) {
                    MethodCard availableCard = (MethodCard)widget;
                    availableCard.setSelected(false);
                }
            selectedMethod.setSelected(true);
            }
        }
    }

    @Override
    public StudyProtocolProxy getSelectedMethod() {
        if (selectedMethod == null)
            return null;
        return selectedMethod.getStudyProtocol();
    }

    @UiHandler("phenotypeGeoChartBtn")
    public void onClickPhenotypeGeoChartBtn(ClickEvent e) {
        if (activePhenotypeChartType == PHENTOYPE_CHART_TYPE.GEOCHART)
            return;
        activePhenotypeChartType = PHENTOYPE_CHART_TYPE.GEOCHART;
        selectPhenotypeChart((Widget) e.getSource());
    }

    @UiHandler("phenotypeColumnChartBtn")
    public void onColumnChartBtn(ClickEvent e) {
        if (activePhenotypeChartType == PHENTOYPE_CHART_TYPE.HISTOGRAM)
            return;
        activePhenotypeChartType = PHENTOYPE_CHART_TYPE.HISTOGRAM;
        selectPhenotypeChart((Widget) e.getSource());
    }

    @UiHandler("phenotypeMotionChartBtn")
    public void onMotionChartBtn(ClickEvent e) {
        if (activePhenotypeChartType == PHENTOYPE_CHART_TYPE.EXPLORER)
            return;
        activePhenotypeChartType = PHENTOYPE_CHART_TYPE.EXPLORER;
        selectPhenotypeChart((Widget) e.getSource());
    }

    private void selectPhenotypeChart(Widget source) {
       phenotypeChartContainer.clear();
       phenotypeGeoChartBtnContainer.removeStyleName(mainRes.style().iconContainer_active());
       phenotypeMotionChartBtnContainer.removeStyleName(mainRes.style().iconContainer_active());
       phenotypeColumnChartBtnContainer.removeStyleName(mainRes.style().iconContainer_active());
       source.getParent().addStyleName(mainRes.style().iconContainer_active());
        drawPhenotypeCharts();
    }

    private void initMissingGenotypesDataGrid() {
        NumberFormat format = NumberFormat.getFormat(NumberFormat.getDecimalFormat().getPattern());

        missingGenotypesDataGrid.addColumn(new Column<TraitProxy,Number>(new NumberCell(format)) {

            @Override
            public Number getValue(TraitProxy object) {
                return object.getObsUnit().getStock().getPassport().getId();
            }
        },"ID");

        missingGenotypesDataGrid.addColumn(new Column<TraitProxy, String>(
                new FlagCell(flagMap)) {
            @Override
            public String getValue(TraitProxy object) {
                String icon = null;
                try {
                    icon = object.getObsUnit().getStock().getPassport().getCollection()
                            .getLocality().getOrigcty();
                } catch (NullPointerException e) {

                }
                return icon;
            }
        }, "Country");
        missingGenotypesDataGrid.addColumn(new Column<TraitProxy, String>(new TextCell()) {

            @Override
            public String getValue(TraitProxy object) {
                return object.getObsUnit().getStock().getPassport().getAccename();
            }
        },"Name");

        missingGenotypesDataGrid.setColumnWidth(0,60, Style.Unit.PX);
        missingGenotypesDataGrid.setColumnWidth(1,120,Style.Unit.PX);
    }
}




