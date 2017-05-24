package org.cpsolver.bgr;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.criteria.Criterion;
import org.cpsolver.ifs.model.Value;

public class TeamAssignment extends Value<Student, TeamAssignment>{
    private Team iTeam;

    public TeamAssignment(Student variable, Team team) {
        super(variable);
        iTeam = team;
    }
    
    public Team getTeam() { return iTeam; }
    
    @Override
    public double toDouble(Assignment<Student, TeamAssignment> assignment) {
        double value = 0.0;
        for (Criterion<Student, TeamAssignment> c: variable().getModel().getCriteria())
            value += c.getWeightedValue(assignment, this, null);
        return value;
    }
}
