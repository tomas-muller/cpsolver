package org.cpsolver.bgr;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.criteria.AbstractCriterion;
import org.cpsolver.ifs.model.GlobalConstraint;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.ifs.util.DataProperties;

public class RequiredFeature extends GlobalConstraint<Student, TeamAssignment> {
    protected Mode iMode;
    protected String iKey = null;
    protected String[] iFallbacks = null;
    
    public static enum Mode {
        HARD,
        SOFT_SUPERVISORS,  
        SOFT_TEAMS,
    }
    
    RequiredFeature(Mode mode, String key, String... fallbacks) {
        iKey = key;
        iFallbacks = fallbacks;
        iMode = mode;
    }
    
    RequiredFeature(String key, String... fallbacks) {
        this(Mode.HARD, key, fallbacks);
    }
    
    public String getKey() { return iKey; }
    
    @Override
    public String getName() {
        return getKey();
    }
    
    public String getProperty(Student student) {
        String value = student.getProperty(iKey);
        if (value != null) return value;
        for (String key: iFallbacks) {
            value = student.getProperty(key);
            if (value != null) return value;
        }
        return value;
    }
    
    @Override
    public void setModel(Model<Student, TeamAssignment> model) {
        super.setModel(model);
        if (iMode != Mode.HARD)
            model.addCriterion(new Criterion(){});
    }
    
    public boolean isDifferent(TeamAssignment value) {
        if (value == null) return false;
        String ft = getProperty(value.getTeam().getTeamLead());
        String f1 = getProperty(value.variable());
        return (ft != null && f1 != null && !f1.equals(ft));
    }
    
    @Override
    public void computeConflicts(Assignment<Student, TeamAssignment> assignment, TeamAssignment value, Set<TeamAssignment> conflicts) {
        String ft = getProperty(value.getTeam().getTeamLead());
        Team.Context context = value.getTeam().getContext(assignment);
        String f1 = getProperty(value.variable());
        if (f1 == null) return;
        switch (iMode) {
            case HARD:
                if (ft != null && !f1.equals(ft)) {
                    conflicts.add(value);
                    return;
                }
                for (Student other: context.getStudents()) {
                    if (other.equals(value.variable())) continue;
                    String f2 = getProperty(other);
                    if (f2 != null && !f2.equals(f1))
                        conflicts.add(assignment.getValue(other));
                }
                break;
            case SOFT_SUPERVISORS:
                if (ft != null && !f1.equals(ft)) {
                    return;
                }
                for (Student other: context.getStudents()) {
                    if (other.equals(value.variable())) continue;
                    if (isDifferent(assignment.getValue(other))) continue;
                    String f2 = getProperty(other);
                    if (f2 != null && !f2.equals(f1))
                        conflicts.add(assignment.getValue(other));
                }
                break;
            case SOFT_TEAMS:
                for (Student other: context.getStudents()) {
                    if (other.equals(value.variable())) continue;
                    String f2 = getProperty(other);
                    if (f2 != null && !f2.equals(f1))
                        conflicts.add(assignment.getValue(other));
                }
                break;
        }
    }
    
    @Override
    public boolean isConsistent(TeamAssignment value1, TeamAssignment value2) {
        if (value1.getTeam().equals(value2.getTeam())) {
            String f1 = getProperty(value1.variable());
            String f2 = getProperty(value2.variable());
            switch (iMode) {
                case HARD:
                case SOFT_TEAMS:
                    return f1 == null || f2 == null || f1.equals(f2);
                case SOFT_SUPERVISORS:
                    if (isDifferent(value1) || isDifferent(value2)) return true;
                    return f1 == null || f2 == null || f1.equals(f2);
            }
        }
        return true;
    }
    
    @Override
    public boolean inConflict(Assignment<Student, TeamAssignment> assignment, TeamAssignment value) {
        String ft = getProperty(value.getTeam().getTeamLead());
        Team.Context context = value.getTeam().getContext(assignment);
        String f1 = getProperty(value.variable());
        if (f1 == null) return false;
        switch (iMode) {
            case HARD:
                if (ft != null && !f1.equals(ft)) return true;
                for (Student other: context.getStudents()) {
                    if (other.equals(value.variable())) continue;
                    String f2 = getProperty(other);
                    if (f2 != null && !f2.equals(f1)) return true;
                }
                break;
            case SOFT_SUPERVISORS:
                if (ft != null && !f1.equals(ft)) return false;
                for (Student other: context.getStudents()) {
                    if (other.equals(value.variable())) continue;
                    if (isDifferent(assignment.getValue(other))) continue;
                    String f2 = getProperty(other);
                    if (f2 != null && !f2.equals(f1)) return true;
                }
                break;
            case SOFT_TEAMS:
                for (Student other: context.getStudents()) {
                    if (other.equals(value.variable())) continue;
                    String f2 = getProperty(other);
                    if (f2 != null && !f2.equals(f1)) return true;
                }
                break;
        }
        return false;
    }
    
    protected class Criterion extends AbstractCriterion<Student, TeamAssignment> {
        
        @Override
        public double getValue(Assignment<Student, TeamAssignment> assignment, TeamAssignment value, Set<TeamAssignment> conflicts) {
            switch (iMode) {
                case SOFT_SUPERVISORS:
                    return (isDifferent(value) ? 1.0 : 0.0);
                case SOFT_TEAMS:
                    return (isDifferent(value) ? 1.0 : 0.0);
                default:
                    return 0.0;
            }
        }
        
        @Override
        public String getWeightName() {
            return "Weight." + getKey();
        }
        
        @Override
        public String getName() {
            return getKey();
        }
        
        @Override
        public double getWeightDefault(DataProperties config) {
            return 1.0;
        }
        
        @Override
        public void getInfo(Assignment<Student, TeamAssignment> assignment, Map<String, String> info) {
            super.getInfo(assignment, info);
            info.put(getKey(), String.valueOf(getValue(assignment)));
            Map<String, Integer> values = new HashMap<String, Integer>();
            for (Student student: getModel().variables()) {
                TeamAssignment ta = assignment.getValue(student);
                if (ta != null && isDifferent(ta)) {
                    String value = getProperty(student) + ">>" + getProperty(ta.getTeam().getTeamLead());
                    Integer count = values.get(value);
                    values.put(value, 1 + (count == null ? 0 : count.intValue()));
                }
            }
            for (String value: values.keySet()) {
                int total = values.get(value);
                info.put(getKey() + " [" + value + "]", String.valueOf(total));
            }
        }
    }
}