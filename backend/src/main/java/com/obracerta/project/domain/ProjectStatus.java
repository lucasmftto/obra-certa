package com.obracerta.project.domain;

public enum ProjectStatus {
    IN_BUDGET, IN_PROGRESS, ON_HOLD, COMPLETED;

    public boolean canTransitionTo(ProjectStatus target) {
        return switch (this) {
            case IN_BUDGET   -> target == IN_PROGRESS;
            case IN_PROGRESS -> target == ON_HOLD || target == COMPLETED;
            case ON_HOLD     -> target == IN_PROGRESS || target == COMPLETED;
            case COMPLETED   -> target == IN_PROGRESS;
        };
    }
}
