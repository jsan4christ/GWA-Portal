package com.gmi.nordborglab.browser.client.manager;

import java.util.List;

import com.gmi.nordborglab.browser.shared.proxy.PhenotypeUploadDataProxy;
import com.gmi.nordborglab.browser.shared.proxy.PhenotypePageProxy;
import com.gmi.nordborglab.browser.shared.proxy.PhenotypeProxy;
import com.gmi.nordborglab.browser.shared.proxy.TraitProxy;
import com.gmi.nordborglab.browser.shared.service.CustomRequestFactory;
import com.gmi.nordborglab.browser.shared.service.PhenotypeRequest;
import com.google.inject.Inject;
import com.google.web.bindery.requestfactory.shared.Receiver;

public class PhenotypeManager extends RequestFactoryManager<PhenotypeRequest> {
	
	public static String[] PATHS = {"statisticTypes","unitOfMeasure","userPermission","traitOntologyTerm"};

	@Inject
	public PhenotypeManager(CustomRequestFactory rf) {
		super(rf);
	}
	
	public void findAll(Receiver<PhenotypePageProxy> receiver,Long id,int start,int size) {
		rf.phenotypeRequest().findPhenotypesByExperiment(id, start, size).with("content.traitOntologyTerm").fire(receiver);
	}

	public void findAll(Receiver<PhenotypePageProxy> receiver,String name,String experiment,String ontology,String protocol,int start,int size) {
		rf.phenotypeRequest().findAll(name,experiment,ontology,protocol, start, size).with("content.traitOntologyTerm","content.experiment").fire(receiver);
	}

	@Override
	public PhenotypeRequest getContext() {
		return rf.phenotypeRequest();
	}
    public void findAllByAcl(Receiver<List<PhenotypeProxy>> receiver, Long experimentId, int permission) {
        rf.phenotypeRequest().findPhenotypesByExperimentAndAcl(experimentId,permission).with(PATHS).fire(receiver);
    }
	public void findOne(Receiver<PhenotypeProxy> receiver, Long id) {
		rf.phenotypeRequest().findPhenotype(id).with(PATHS).fire(receiver);
	}
	
	public void findAllTraitValues(Receiver<List<TraitProxy>> receiver,Long phenotypeId,Long alleleAssayId ,Long statisticTypeId) {
		rf.traitRequest().findAllTraitValues(phenotypeId, alleleAssayId, statisticTypeId).with("obsUnit.stock.passport.collection.locality","statisticType").fire(receiver);
	}

	public void findAllTraitValuesByType(Long phenotypeId,Long statisticTypeId,
			Receiver<List<TraitProxy>> receiver) {
		rf.traitRequest().findAllTraitValuesByStatisticType(phenotypeId,statisticTypeId).with("obsUnit.stock.passport.collection.locality","obsUnit.stock.passport.alleleAssays").fire(receiver);
	}


    public void savePhenotypeUploadData(Receiver<Long> receiver,Long experimentId,PhenotypeUploadDataProxy data)  {
        PhenotypeRequest ctx = rf.phenotypeRequest();
        ctx.savePhenotypeUploadData(experimentId,data).fire(receiver);
    }

    public void findAllByOntology(Receiver<List<PhenotypeProxy>> receiver,String type, String acc, boolean checkChilds) {
        getContext().findAllByOntology(type,acc,checkChilds).with("traitOntologyTerm","experiment").fire(receiver);
    }
}
