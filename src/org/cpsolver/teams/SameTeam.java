package org.cpsolver.teams;

import java.util.HashSet;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.context.AssignmentConstraintContext;
import org.cpsolver.ifs.assignment.context.ConstraintWithContext;

public class SameTeam extends ConstraintWithContext<Student, TeamAssignment, SameTeam.Context> {

    @Override
    public void computeConflicts(Assignment<Student, TeamAssignment> assignment, TeamAssignment value, Set<TeamAssignment> conflicts) {
        Context context = getContext(assignment);
        boolean same = (context.getTeam() == null || context.getTeam().equals(value.getTeam()));
        if (!same)
            conflicts.addAll(context.getAssignments());
    }
    
    @Override
    public boolean inConflict(Assignment<Student, TeamAssignment> assignment, TeamAssignment value) {
        Context context = getContext(assignment);
        boolean same = (context.getTeam() == null || context.getTeam().equals(value.getTeam()));
        return same ? false : !context.getAssignments().isEmpty();
    }

    @Override
    public boolean isConsistent(TeamAssignment first, TeamAssignment second) {
        return first.getTeam().equals(second.getTeam());
    }
    
    @Override
    public Context createAssignmentContext(Assignment<Student, TeamAssignment> assignment) {
        return new Context(assignment);
    }
    
    public class Context implements AssignmentConstraintContext<Student, TeamAssignment> {
        private Team iTeam = null;
        private Set<TeamAssignment> iAssignments = new HashSet<TeamAssignment>();
        
        public Context(Assignment<Student, TeamAssignment> assignment) {
            for (Student student: variables()) {
                TeamAssignment ta = assignment.getValue(student);
                if (ta != null) {
                    iTeam = ta.getTeam();
                    iAssignments.add(ta);
                }
            }
        }

        @Override
        public void assigned(Assignment<Student, TeamAssignment> assignment, TeamAssignment ta) {
            iAssignments.add(ta);
            iTeam = ta.getTeam();
        }
        
        @Override
        public void unassigned(Assignment<Student, TeamAssignment> assignment, TeamAssignment ta) {
            iAssignments.remove(ta);
            if (iAssignments.isEmpty()) iTeam = null;
        }
        
        public Set<TeamAssignment> getAssignments() {
            return iAssignments;
        }
        
        public Team getTeam() {
            return iTeam;
        }
    }

}