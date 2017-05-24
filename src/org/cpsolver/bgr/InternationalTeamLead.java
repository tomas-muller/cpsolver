package org.cpsolver.bgr;

import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.GlobalConstraint;

public class InternationalTeamLead extends GlobalConstraint<Student, TeamAssignment> {

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }
        
    @Override
    public void computeConflicts(Assignment<Student, TeamAssignment> assignment, TeamAssignment value, Set<TeamAssignment> conflicts) {
        // not international team lead -> no international students
        if (!value.getTeam().getTeamLead().isInternational() && value.variable().isInternational())
            conflicts.add(value);
    }
    
    @Override
    public boolean inConflict(Assignment<Student, TeamAssignment> assignment, TeamAssignment value) {
        return !value.getTeam().getTeamLead().isInternational() && value.variable().isInternational();
    }
}