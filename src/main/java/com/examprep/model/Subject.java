package com.examprep.model;

public class Subject {

    private Long id;
    private String name;
    private String description;
    private boolean professional;
    private boolean subProfessional;

    public Subject() {
    }

    public Subject(Long id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isProfessional() {
        return professional;
    }

    public void setProfessional(boolean professional) {
        this.professional = professional;
    }

    public boolean isSubProfessional() {
        return subProfessional;
    }

    public void setSubProfessional(boolean subProfessional) {
        this.subProfessional = subProfessional;
    }
}
