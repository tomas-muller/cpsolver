package org.cpsolver.teams;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.criteria.AbstractCriterion;
import org.cpsolver.ifs.model.Constraint;
import org.cpsolver.ifs.util.DataProperties;

public class TeamSize extends AbstractCriterion<Student, TeamAssignment> {
    
    public TeamSize() {
    }
    
    @Override
    public String getWeightName() {
        return "Weight.TeamSize";
    }
    
    @Override
    public double getWeightDefault(DataProperties config) {
        return 1.0;
    }
    
    public double similar(Student a, Student b) {
        return 1.0;//Math.min(a.getWeight(), b.getWeight());
    }

    @Override
    public double getValue(Assignment<Student, TeamAssignment> assignment, TeamAssignment value, Set<TeamAssignment> conflicts) {
        double similar = 0;
        for (Student student: value.getTeam().getContext(assignment).getStudents()) {
            if (student.equals(value.variable())) continue;
            similar += similar(student, value.variable());
        }
        return similar;
    }
    
    @Override
    public double getValue(Assignment<Student, TeamAssignment> assignment, Collection<Student> variables) {
        return super.getValue(assignment, variables) / 2.0;
    }
    
    @Override
    public void getInfo(Assignment<Student, TeamAssignment> assignment, Map<String, String> info) {
        super.getInfo(assignment, info);
        int teams = 0;
        for (Constraint<Student, TeamAssignment> c: getModel().constraints())
            if (c instanceof Team) teams ++;
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        double rms = 0;
        double avg = ((double)assignment.nrAssignedVariables()) / teams;
        int penalty = 0;
        for (Constraint<Student, TeamAssignment> c: getModel().constraints())
            if (c instanceof Team) {
                Team t = (Team)c;
                int cnt = t.getContext(assignment).getStudentCount();
                rms += Math.pow(cnt - avg, 2.0);
                if (cnt < min) min = cnt;
                if (cnt > max) max = cnt;
                penalty += cnt * (cnt - 1) / 2;
            }
        info.put("Team Size", sDoubleFormat.format(avg) + " +/- " + sDoubleFormat.format(Math.sqrt(rms / teams)) + " (" + min + ".." + max + ") p=" + penalty);
    }
}
