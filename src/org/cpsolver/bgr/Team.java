package org.cpsolver.bgr;

import java.util.HashSet;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.context.AbstractClassWithContext;
import org.cpsolver.ifs.assignment.context.AssignmentConstraintContext;
import org.cpsolver.ifs.model.Model;

public class Team extends AbstractClassWithContext<Student, TeamAssignment, Team.Context>{
    private Student iLead;
    private Model<Student, TeamAssignment> iModel;
    
    public Team(Student lead) {
        iLead = lead;
    }
    
    public String getName() {
        return getTeamLead().getName();
    }
    
    public Student getTeamLead() {
        return iLead;
    }
    
    @Override
    public int hashCode() { return getTeamLead().hashCode(); }
    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Team)) return false;
        return getTeamLead().equals(((Team)o).getTeamLead());
    }
    
    public class Context implements AssignmentConstraintContext<Student, TeamAssignment>{
        private Set<Student> iStudents = new HashSet<Student>();
        private Set<Student> iInternationalStudents = new HashSet<Student>();
        
        public Context(Assignment<Student, TeamAssignment> assignment) {
            for (Student student: getModel().variables()) {
                TeamAssignment ta = assignment.getValue(student);
                if (ta != null && ta.getTeam().equals(Team.this)) {
                    iStudents.add(student);
                    if (student.isInternational())
                        iInternationalStudents.add(student);
                }
            }
        }

        @Override
        public void assigned(Assignment<Student, TeamAssignment> assignment, TeamAssignment ta) {
            // if (!ta.getTeam().equals(Team.this)) return;
            iStudents.add(ta.variable());
            if (ta.variable().isInternational())
                iInternationalStudents.add(ta.variable());
        }

        @Override
        public void unassigned(Assignment<Student, TeamAssignment> assignment, TeamAssignment ta) {
            // if (!ta.getTeam().equals(Team.this)) return;
            iStudents.remove(ta.variable());
            if (ta.variable().isInternational())
                iInternationalStudents.remove(ta.variable());
        }
        
        public Set<Student> getStudents() { return iStudents; }
        public Set<Student> getInternationalStudents() { return iInternationalStudents; }
    }

    @Override
    public Context createAssignmentContext(Assignment<Student, TeamAssignment> assignment) {
        return new Context(assignment);
    }
    
    public void setModel(Model<Student, TeamAssignment> model) {
        iModel = model;
    }

    @Override
    public Model<Student, TeamAssignment> getModel() {
        return iModel;
    }
    
    @Override
    public String toString() {
        return getTeamLead().toString();
    }
}
