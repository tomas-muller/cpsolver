package org.cpsolver.teams;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.context.AssignmentConstraintContext;
import org.cpsolver.ifs.assignment.context.ConstraintWithContext;

public class DifferentTeam extends ConstraintWithContext<Student, TeamAssignment, DifferentTeam.Context> {

    @Override
    public void computeConflicts(Assignment<Student, TeamAssignment> assignment, TeamAssignment value, Set<TeamAssignment> conflicts) {
        TeamAssignment conflict = getContext(assignment).getAssignment(value.getTeam());
        if (conflict != null && !conflict.variable().equals(value.variable())) {
            conflicts.add(conflict);
        }
    }
    
    @Override
    public boolean inConflict(Assignment<Student, TeamAssignment> assignment, TeamAssignment value) {
        TeamAssignment conflict = getContext(assignment).getAssignment(value.getTeam());
        return conflict != null && !conflict.variable().equals(value.variable());
    }

    @Override
    public boolean isConsistent(TeamAssignment first, TeamAssignment second) {
        return !first.getTeam().equals(second.getTeam());
    }
    
    @Override
    public Context createAssignmentContext(Assignment<Student, TeamAssignment> assignment) {
        return new Context(assignment);
    }
    
    public class Context implements AssignmentConstraintContext<Student, TeamAssignment> {
        private Map<Team, TeamAssignment> iAssignment = new HashMap<Team, TeamAssignment>();
        
        public Context(Assignment<Student, TeamAssignment> assignment) {
            for (Student student: variables()) {
                TeamAssignment ta = assignment.getValue(student);
                if (ta != null)
                    iAssignment.put(ta.getTeam(), ta);
            }
        }

        @Override
        public void assigned(Assignment<Student, TeamAssignment> assignment, TeamAssignment ta) {
            iAssignment.put(ta.getTeam(), ta);
        }
        
        @Override
        public void unassigned(Assignment<Student, TeamAssignment> assignment, TeamAssignment ta) {
            iAssignment.remove(ta.getTeam());
        }
        
        public TeamAssignment getAssignment(Team team) {
            return iAssignment.get(team);
        }
    }

}
