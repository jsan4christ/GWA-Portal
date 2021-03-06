package com.gmi.nordborglab.browser.client.mvp.diversity.meta.candidategenelist.detail;

import com.gmi.nordborglab.browser.client.events.DisplayNotificationEvent;
import com.gmi.nordborglab.browser.client.events.FacetSearchChangeEvent;
import com.gmi.nordborglab.browser.client.events.LoadCandidateGeneListEvent;
import com.gmi.nordborglab.browser.client.events.LoadingIndicatorEvent;
import com.gmi.nordborglab.browser.client.events.PermissionDoneEvent;
import com.gmi.nordborglab.browser.client.gin.ClientModule;
import com.gmi.nordborglab.browser.client.manager.EnrichmentProvider;
import com.gmi.nordborglab.browser.client.manager.SearchManager;
import com.gmi.nordborglab.browser.client.mvp.diversity.DiversityPresenter;
import com.gmi.nordborglab.browser.client.mvp.diversity.meta.candidategenelist.list.CandidateGeneListView;
import com.gmi.nordborglab.browser.client.mvp.widgets.facets.FacetSearchPresenterWidget;
import com.gmi.nordborglab.browser.client.mvp.widgets.permissions.PermissionDetailPresenter;
import com.gmi.nordborglab.browser.client.place.NameTokens;
import com.gmi.nordborglab.browser.client.security.CurrentUser;
import com.gmi.nordborglab.browser.client.util.DataTableUtils;
import com.gmi.nordborglab.browser.shared.proxy.CandidateGeneListProxy;
import com.gmi.nordborglab.browser.shared.proxy.FacetProxy;
import com.gmi.nordborglab.browser.shared.proxy.GenePageProxy;
import com.gmi.nordborglab.browser.shared.proxy.annotation.GeneProxy;
import com.gmi.nordborglab.browser.shared.service.CustomRequestFactory;
import com.gmi.nordborglab.browser.shared.service.MetaAnalysisRequest;
import com.gmi.nordborglab.browser.shared.util.ConstEnums;
import com.google.common.base.Predicate;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.Range;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.google.web.bindery.requestfactory.shared.RequestContext;
import com.google.web.bindery.requestfactory.shared.ServerFailure;
import com.googlecode.gwt.charts.client.DataTable;
import com.gwtplatform.mvp.client.HasUiHandlers;
import com.gwtplatform.mvp.client.Presenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.NameToken;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.annotations.ProxyEvent;
import com.gwtplatform.mvp.client.presenter.slots.PermanentSlot;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import com.gwtplatform.mvp.client.proxy.ProxyPlace;
import com.gwtplatform.mvp.shared.proxy.PlaceRequest;
import org.gwtbootstrap3.client.ui.constants.IconType;

import javax.annotation.Nullable;
import javax.validation.ConstraintViolation;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: uemit.seren
 * Date: 25.09.13
 * Time: 13:30
 * To change this template use File | Settings | File Templates.
 */
public class CandidateGeneListDetailPresenter extends Presenter<CandidateGeneListDetailPresenter.MyView, CandidateGeneListDetailPresenter.MyProxy> implements CandidateGeneListDetailUiHandlers {

    public enum STATS {CHR, ANNOTATION, STRAND}

    public interface MyView extends View, HasUiHandlers<CandidateGeneListDetailUiHandlers> {

        HasData<GeneProxy> getGenesDisplay();

        CandidateGeneListDetailView.CandidateGeneListDisplayDriver getDisplayDriver();

        void showEditPopup(boolean show);

        void showDeletePopup();

        void setShareTooltip(String toopltipMsg, IconType icon);

        void showShareBtn(boolean show);

        void showActionBtns(boolean show);

        CandidateGeneListView.CandidateGeneListEditDriver getEditDriver();

        void showPermissionPanel(boolean show);

        void phaseInPublication(GeneProxy gene);

        HasText getSearchBox();

        void enableAddBtn(boolean enable);

        void setStatsData(DataTable dataTable, STATS stat);

        void refreshStats();

        void setUploadActionUrl(String url);

        void setEnrichmentCount(int count);

        void setCandidateGeneListId(Long id);
    }

    @ProxyCodeSplit
    @NameToken(NameTokens.candidateGeneListDetail)
    public interface MyProxy extends ProxyPlace<CandidateGeneListDetailPresenter> {
    }

    static final PermanentSlot<CandidateGeneListEnrichmentPresenterWidget> SLOT_ENRICHMENT = new PermanentSlot<>();
    private CandidateGeneListProxy candidateGeneList;
    private final PermissionDetailPresenter permissionDetailPresenter;
    private final Receiver<CandidateGeneListProxy> receiver = new Receiver<CandidateGeneListProxy>() {
        public void onSuccess(CandidateGeneListProxy response) {
            fireEvent(new LoadingIndicatorEvent(false));
            candidateGeneList = response;
            getView().getDisplayDriver().display(candidateGeneList);
            getView().showEditPopup(false);
        }

        public void onFailure(ServerFailure error) {
            fireEvent(new LoadingIndicatorEvent(false));
            DisplayNotificationEvent.fireError(getEventBus(), "Error while saving", error.getMessage());
            onEdit();
        }

        public void onConstraintViolation(
                Set<ConstraintViolation<?>> violations) {
            fireEvent(new LoadingIndicatorEvent(false));
            super.onConstraintViolation(violations);
        }
    };

    private final CustomRequestFactory rf;
    private final PlaceManager placeManager;
    private final CurrentUser currentUser;
    private List<FacetProxy> statsFacets;

    static final PermanentSlot<PermissionDetailPresenter> SLOT_PERMISSION = new PermanentSlot<>();
    private GenePageProxy genesPage;
    private final BiMap<ConstEnums.GENE_FILTER, List<String>> filter2Annotation;
    private final CandidateGeneListEnrichmentPresenterWidget candidateGeneListEnrichmentPresenter;
    private int enrichmentCount = 0;
    private boolean refreshEnrichmentWidget = false;
    private static final Map<String, String> FACET_MAP = ImmutableMap.<String, String>builder()
            .put(ConstEnums.GENE_FILTER.ALL.name(), "All")
            .put(ConstEnums.GENE_FILTER.PROTEIN.name(), "Protein")
            .put(ConstEnums.GENE_FILTER.PSEUDO.name(), "Pseudo")
            .put(ConstEnums.GENE_FILTER.TRANSPOSON.name(), "Transposon").build();
    private final FacetSearchPresenterWidget facetSearchPresenterWidget;
    private final SearchManager searchManager;

    private final AsyncDataProvider<GeneProxy> genesDataProvider = new AsyncDataProvider<GeneProxy>() {
        @Override
        protected void onRangeChanged(HasData<GeneProxy> display) {
            requestGenes(display, null);
        }
    };


    private final EnrichmentProvider dataProvider;

    @Inject
    public CandidateGeneListDetailPresenter(EventBus eventBus, MyView view, MyProxy proxy,
                                            final PermissionDetailPresenter permissionDetailPresenter,
                                            final CustomRequestFactory rf, final PlaceManager placeManager,
                                            final CurrentUser currentUser,
                                            final ClientModule.AssistedInjectionFactory factory,
                                            final FacetSearchPresenterWidget facetSearchPresenterWidget,
                                            final SearchManager searchManager) {
        super(eventBus, view, proxy, DiversityPresenter.SLOT_CONTENT);
        filter2Annotation = new ImmutableBiMap.Builder<ConstEnums.GENE_FILTER, List<String>>()
                .put(ConstEnums.GENE_FILTER.PROTEIN, Lists.newArrayList("gene"))
                .put(ConstEnums.GENE_FILTER.TRANSPOSON, Lists.newArrayList("transposable_element", "transposable_element_gene"))
                .put(ConstEnums.GENE_FILTER.PSEUDO, Lists.newArrayList("pseudogene")).build();
        this.permissionDetailPresenter = permissionDetailPresenter;
        this.facetSearchPresenterWidget = facetSearchPresenterWidget;
        this.searchManager = searchManager;
        // Required because otherwise conflicts with the url parameter of the facet widget inside of the enrichments tab
        facetSearchPresenterWidget.setDefaultFilterParam("gene_filter");
        facetSearchPresenterWidget.setDefaultQueryParam("gene_query");
        facetSearchPresenterWidget.setSearchBoxVisible(false);
        facetSearchPresenterWidget.setDefaultFilter(ConstEnums.GENE_FILTER.ALL.name());
        facetSearchPresenterWidget.initFixedFacets(FACET_MAP);
        dataProvider = factory.createEnrichmentProvider(ConstEnums.ENRICHMENT_TYPE.CANDIDATE_GENE_LIST);
        this.candidateGeneListEnrichmentPresenter = factory.createCandidateGeneListEnrichmentPresenter(dataProvider);
        this.currentUser = currentUser;
        this.rf = rf;
        this.placeManager = placeManager;
        getView().setUiHandlers(this);

        genesDataProvider.addDataDisplay(getView().getGenesDisplay());
    }

    private String geneId = null;


    @Override
    protected void onBind() {
        super.onBind();    //To change body of overridden methods use File | Settings | File Templates.
        setInSlot(SLOT_PERMISSION, permissionDetailPresenter);
        setInSlot(SLOT_ENRICHMENT, candidateGeneListEnrichmentPresenter);
        setInSlot(FacetSearchPresenterWidget.SLOT_CONTENT, facetSearchPresenterWidget);
        registerHandler(getEventBus().addHandlerToSource(PermissionDoneEvent.TYPE, permissionDetailPresenter, new PermissionDoneEvent.Handler() {
            @Override
            public void onPermissionDone(PermissionDoneEvent event) {
                rf.metaAnalysisRequest().findOneCandidateGeneList(candidateGeneList.getId()).with("userPermission", "ownerUser").to(new Receiver<CandidateGeneListProxy>() {
                    @Override
                    public void onSuccess(CandidateGeneListProxy response) {
                        candidateGeneList = response;
                        enrichmentCount = candidateGeneList.getEnrichmentCount();
                        dataProvider.setEntityId(candidateGeneList.getId());
                        getView().showPermissionPanel(false);
                        refreshView();
                    }
                }).fire();
            }
        }));
        registerHandler(getEventBus().addHandlerToSource(FacetSearchChangeEvent.TYPE, facetSearchPresenterWidget, new FacetSearchChangeEvent.Handler() {

            @Override
            public void onChanged(FacetSearchChangeEvent event) {
                getView().getGenesDisplay().setVisibleRangeAndClearData(getView().getGenesDisplay().getVisibleRange(), true);
            }
        }));
    }

    @Override
    public boolean useManualReveal() {
        return true;
    }

    private void requestGenes(final HasData<GeneProxy> display, final GeneProxy newGeneProxy) {
        if (candidateGeneList == null)
            return;
        if (genesPage == null) {
            fireEvent(new LoadingIndicatorEvent(true));
            Receiver<GenePageProxy> receiver = new Receiver<GenePageProxy>() {
                @Override
                public void onSuccess(GenePageProxy response) {
                    fireEvent(new LoadingIndicatorEvent(false));
                    genesPage = response;
                    statsFacets = genesPage.getStatsFacets();
                    //FIXME workaround until facetSearchPresenterWidget properly supports counts
                    facetSearchPresenterWidget.initFixedFacets(FACET_MAP);
                    facetSearchPresenterWidget.displayFacets(genesPage.getFacets());
                    displayStats();
                    getView().enableAddBtn(genesPage.getContents().size() == 0 && geneId != null);
                    filterAndDisplayGenes(newGeneProxy);
                }
            };
            Range range = display.getVisibleRange();
            rf.metaAnalysisRequest().getGenesInCandidateGeneList(candidateGeneList.getId(), ConstEnums.GENE_FILTER.valueOf(facetSearchPresenterWidget.getFilter()), geneId, range.getStart(), range.getLength()).fire(receiver);
        } else {
            filterAndDisplayGenes(newGeneProxy);
        }
    }

    private void displayStats() {
        for (FacetProxy facet : statsFacets) {
            STATS stat = STATS.valueOf(facet.getName().toUpperCase());
            DataTable dataTable = DataTableUtils.createFroMFacets(facet);
            getView().setStatsData(dataTable, stat);
        }
        getView().refreshStats();
    }

    @Override
    public void prepareFromRequest(PlaceRequest placeRequest) {
        super.prepareFromRequest(placeRequest);
        LoadingIndicatorEvent.fire(this, true);
        try {
            Long id = Long.valueOf(placeRequest.getParameter("id", null));
            if (candidateGeneList == null || !candidateGeneList.getId().equals(id)) {
                genesPage = null;
                MetaAnalysisRequest ctx = rf.metaAnalysisRequest();
                ctx.findOneCandidateGeneList(id).with("userPermission", "ownerUser").to(new Receiver<CandidateGeneListProxy>() {
                    @Override
                    public void onSuccess(CandidateGeneListProxy response) {
                        candidateGeneList = response;
                        enrichmentCount = candidateGeneList.getEnrichmentCount();
                        dataProvider.setEntityId(candidateGeneList.getId());
                        candidateGeneListEnrichmentPresenter.refresh();
                    }
                });
                ctx.fire(new Receiver<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        getProxy().manualReveal(CandidateGeneListDetailPresenter.this);
                    }

                    @Override
                    public void onFailure(ServerFailure error) {
                        getProxy().manualRevealFailed();
                        placeManager.revealPlace(new PlaceRequest.Builder().nameToken(NameTokens.candidateGeneList).build());
                    }
                });
            } else {
                getProxy().manualReveal(CandidateGeneListDetailPresenter.this);
            }
        } catch (NumberFormatException e) {
            getProxy().manualRevealFailed();
            placeManager.revealPlace(new PlaceRequest.Builder().nameToken(NameTokens.candidateGeneList).build());
        }
    }

    @Override
    protected void onReset() {
        super.onReset();    //To change body of overridden methods use File | Settings | File Templates.
        fireEvent(new LoadingIndicatorEvent(false));
        refreshView();
        requestGenes(getView().getGenesDisplay(), null);
        getView().setCandidateGeneListId(candidateGeneList.getId());
    }

    @Override
    public void onSearchForGene(final String request, final SearchManager.SearchCallback callback) {
        searchManager.searchGeneByTerm(request, callback);
    }

    @Override
    public void onSelectGene(String gene) {
        this.geneId = gene;
        genesPage = null;
        getView().getGenesDisplay().setVisibleRangeAndClearData(getView().getGenesDisplay().getVisibleRange(), true);
    }

    @Override
    public void onAddGene() {
        rf.metaAnalysisRequest().addGeneToCandidateGeneList(candidateGeneList, geneId).fire(new Receiver<GeneProxy>() {
            @Override
            public void onSuccess(GeneProxy response) {
                genesPage = null;
                geneId = null;
                resetEnrichmentCountAndUpdateView();
                requestGenes(getView().getGenesDisplay(), response);
                getView().getSearchBox().setText("");
                getView().enableAddBtn(false);
            }
        });
    }

    @Override
    public void onEdit() {
        MetaAnalysisRequest ctx = rf.metaAnalysisRequest();
        getView().getEditDriver().edit(candidateGeneList, ctx);
        ctx.saveCandidateGeneList(candidateGeneList).with("userPermission").to(receiver);
        getView().showEditPopup(true);
    }

    @Override
    public void onDelete() {
        getView().showDeletePopup();
    }

    @Override
    public void onConfirmDelete() {
        fireEvent(new LoadingIndicatorEvent(true, "Deleting..."));
        rf.metaAnalysisRequest().deleteCandidateGeneList(candidateGeneList).fire(new Receiver<Void>() {
            @Override
            public void onSuccess(Void response) {
                fireEvent(new LoadingIndicatorEvent(false));
                PlaceRequest request = null;
                if (placeManager.getHierarchyDepth() <= 1) {
                    request = new PlaceRequest.Builder().nameToken(NameTokens.candidateGeneList).build();
                } else {
                    request = placeManager.getCurrentPlaceHierarchy().get(placeManager.getHierarchyDepth() - 2);
                }
                candidateGeneList = null;
                placeManager.revealPlace(request);
                //TODO fire event to refresh the list
            }

            @Override
            public void onFailure(ServerFailure error) {
                fireEvent(new LoadingIndicatorEvent(false));
                super.onFailure(error);    //To change body of overridden methods use File | Settings | File Templates.
            }
        });
    }

    @Override
    public void onCancel() {
        getView().showEditPopup(false);
    }

    @Override
    public void onSave() {
        RequestContext req = getView().getEditDriver().flush();
        fireEvent(new LoadingIndicatorEvent(true, "Saving..."));
        req.fire();
    }

    @Override
    public void onShare() {
        getView().showPermissionPanel(true);
        permissionDetailPresenter.setDomainObject(candidateGeneList, placeManager.buildHistoryToken(placeManager.getCurrentPlaceRequest()));
    }

    @Override
    public void onDeleteGene(GeneProxy gene) {
        rf.metaAnalysisRequest().removeGeneFromCandidateGeneList(candidateGeneList, gene.getName()).fire(new Receiver<Void>() {
            @Override
            public void onSuccess(Void response) {
                genesPage = null;
                resetEnrichmentCountAndUpdateView();
                requestGenes(getView().getGenesDisplay(), null);
            }
        });
    }

    @Override
    public void refresh() {
        genesPage = null;
        resetEnrichmentCountAndUpdateView();
        getView().getGenesDisplay().setVisibleRangeAndClearData(getView().getGenesDisplay().getVisibleRange(), true);
    }

    private void refreshView() {
        getView().getDisplayDriver().display(candidateGeneList);
        getView().showActionBtns(currentUser.hasEdit(candidateGeneList));
        getView().showShareBtn(currentUser.hasAdmin(candidateGeneList));
        getView().setEnrichmentCount(enrichmentCount);
        getView().setUploadActionUrl(GWT.getHostPageBaseURL() + "/provider/candidategenelist/" + candidateGeneList.getId() + "/upload");

        String toolTipText = "Public - Anyone on the Internet can find and access";
        IconType toolTipIcon = IconType.GLOBE;
        if (!candidateGeneList.isPublic()) {
            toolTipText = "Private - Only people explicitly granted permission can access";
            toolTipIcon = IconType.LOCK;

        }
        getView().setShareTooltip(toolTipText, toolTipIcon);
    }

    @ProxyEvent
    public void onLoadCandidateGeneList(LoadCandidateGeneListEvent event) {
        candidateGeneList = event.getCandidateGeneList();
        genesPage = null;
        PlaceRequest request = new PlaceRequest.Builder()
                .nameToken(NameTokens.candidateGeneListDetail)
                .with("id", candidateGeneList.getId().toString()).build();
        placeManager.revealPlace(request);
    }


    private void filterAndDisplayGenes(final GeneProxy newGeneProxy) {
        Range range = getView().getGenesDisplay().getVisibleRange();
        if (newGeneProxy != null) {
            if (Iterables.find(genesPage.getContents(), new Predicate<GeneProxy>() {
                @Override
                public boolean apply(@Nullable GeneProxy geneProxy) {
                    return newGeneProxy.equals(geneProxy);
                }
            }, null) == null) {
                genesPage.getContents().add(0, newGeneProxy);
            }
        }
        final ConstEnums.GENE_FILTER filter = ConstEnums.GENE_FILTER.valueOf(facetSearchPresenterWidget.getFilter());
        List<GeneProxy> filteredList = ImmutableList.copyOf(Iterables.filter(genesPage.getContents(), new Predicate<GeneProxy>() {
            @Override
            public boolean apply(@Nullable GeneProxy geneProxy) {
                if (geneProxy == null)
                    return false;
                if (!filter2Annotation.containsKey(filter))
                    return true;
                boolean found = false;
                for (String key : filter2Annotation.get(filter)) {
                    if (key.equalsIgnoreCase(geneProxy.getAnnotation())) {
                        found = true;
                        break;
                    }
                }
                return found;
            }
        }));
        genesDataProvider.updateRowCount(filteredList.size(), true);
        genesDataProvider.updateRowData(range.getStart(), filteredList);
        if (newGeneProxy != null) {
            getView().phaseInPublication(newGeneProxy);
        }
    }


    private void resetEnrichmentCountAndUpdateView() {
        boolean refreshRequired = enrichmentCount > 0;
        enrichmentCount = 0;
        getView().setEnrichmentCount(enrichmentCount);
        if (refreshRequired) {
            candidateGeneListEnrichmentPresenter.refresh();
            refreshEnrichmentWidget = false;
        }

    }

}
