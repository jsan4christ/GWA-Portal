package com.gmi.nordborglab.browser.server.domain.phenotype;

import java.util.*;

import javax.persistence.AttributeOverride;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.gmi.nordborglab.browser.server.domain.BaseEntity;
import com.gmi.nordborglab.browser.server.domain.SecureEntity;
import com.gmi.nordborglab.browser.server.domain.acl.AclTraitUomIdentity;
import com.gmi.nordborglab.browser.server.domain.observation.Experiment;
import com.gmi.nordborglab.browser.server.security.CustomAccessControlEntry;
import com.gmi.nordborglab.jpaontology.model.Term;
import com.google.common.collect.Iterables;

@Entity
@Table(name = "div_trait_uom", schema = "phenotype")
@AttributeOverride(name = "id", column = @Column(name = "div_trait_uom_id"))
@SequenceGenerator(name = "idSequence", sequenceName = "phenotype.div_trait_uom_div_trait_uom_id_seq")
public class TraitUom extends SecureEntity {

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "div_unit_of_measure_id")
    private UnitOfMeasure unitOfMeasure;

    @OneToOne(mappedBy = "traitUom", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private AclTraitUomIdentity acl;

    @OneToMany(mappedBy = "traitUom", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<Trait> traits = new HashSet<Trait>();

    private String local_trait_name;
    private String trait_protocol;
    @Column(name = "to_accession")
    private String toAccession;
    @Column(name = "eo_accession")
    private String eoAccession;

    @Transient
    private Experiment experiment;

    @Transient
    private Long numberOfObsUnits;

    @Transient
    private Long numberOfStudies;

    @Transient
    private Term traitOntologyTerm;

    @Transient
    private List<StatisticType> statisticTypes;

    @Transient
    private List<Long> statisticTypeTraitCounts;

    public TraitUom() {
    }

    public AclTraitUomIdentity getAcl() {
        return acl;
    }

    public Experiment getExperiment() {
        ///TODO change database schema for more efficient access
        if (experiment == null) {
            Trait trait = Iterables.get(traits, 0);
            experiment = trait.getObsUnit().getExperiment();
        }
        return experiment;
    }

    public Set<Trait> getTraits() {
        return Collections.unmodifiableSet(traits);
    }

    public List<StatisticType> getStatisticTypes() {
        return statisticTypes;
    }

    public void setStatisticTypes(List<StatisticType> statisticTypes) {
        this.statisticTypes = statisticTypes;
    }

    public UnitOfMeasure getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(UnitOfMeasure unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public String getLocalTraitName() {
        return local_trait_name;
    }

    public void setLocalTraitName(String localTraitName) {
        this.local_trait_name = localTraitName;
    }

    public String getTraitProtocol() {
        return trait_protocol;
    }

    public void setTraitProtocol(String traitProtocol) {
        this.trait_protocol = traitProtocol;
    }

    public String getToAccession() {
        return toAccession;
    }

    public void setToAccession(String toAccession) {
        this.toAccession = toAccession;
    }

    public String getEoAccession() {
        return eoAccession;
    }

    public void setEoAccession(String eoAccession) {
        this.eoAccession = eoAccession;
    }

    public Long getNumberOfObsUnits() {
        return numberOfObsUnits;
    }

    public void setNumberOfObsUnits(Long count) {
        this.numberOfObsUnits = count;
    }

    public Long getNumberOfStudies() {
        return numberOfStudies;
    }

    public void setNumberOfStudies(Long numberOfStudies) {
        this.numberOfStudies = numberOfStudies;
    }

    public Term getTraitOntologyTerm() {
        return traitOntologyTerm;
    }

    public void setTraitOntologyTerm(Term traitOntologyTerm) {
        this.traitOntologyTerm = traitOntologyTerm;
    }

    public void addTrait(Trait trait) {
        traits.add(trait);
        trait.setTraitUom(this);
    }


    public void setStatisticTypeTraitCounts(List<Long> statisticTypeTraitCounts) {
        this.statisticTypeTraitCounts = statisticTypeTraitCounts;
    }

    public List<Long> getStatisticTypeTraitCounts() {
        return statisticTypeTraitCounts;
    }
}
