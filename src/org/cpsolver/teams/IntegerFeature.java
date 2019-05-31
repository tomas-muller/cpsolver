package org.cpsolver.teams;

import java.util.Map;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.model.Constraint;
import org.cpsolver.ifs.solver.Solver;

public class IntegerFeature extends Feature {
    public int iMaximum = 0;
    
    public IntegerFeature(String key, String[] fallbacks) {
        super(key, fallbacks);
    }
    
    public IntegerFeature(String... keyWithFallbacks) {
        super(keyWithFallbacks);
    }
    
    @Override
    public boolean init(Solver<Student, TeamAssignment> solver) {
        iMaximum = 0;
        for (Student student: getModel().variables())
            iMaximum = Math.max(iMaximum, getProperty(student, 0));
        return super.init(solver);
    }
    
    public Integer getProperty(Student student, int defaultValue) {
        String value = getProperty(student);
        return (value == null || value.isEmpty() ? defaultValue : Integer.parseInt(value));
    }
    
    @Override
    public double similar(Student a, Student b) {
        double diff = Math.abs(getProperty(a, 0) - getProperty(b, 0));
        return (iMaximum - diff) / iMaximum;
    }
    
    @Override
    public void getExtendedInfo(Assignment<Student, TeamAssignment> assignment, Map<String, String> info) {
        super.getExtendedInfo(assignment, info);
        double min = Double.MAX_VALUE, max = Double.MIN_VALUE;
        double total = 0;
        int teams = 0;
        for (Constraint<Student, TeamAssignment> c: getModel().constraints()) {
            if (c instanceof Team) {
                Team t = (Team)c;
                int nrStudents = 0;
                double value = 0.0;
                int mi = Integer.MAX_VALUE, mx = Integer.MIN_VALUE;
                for (Student s: t.getContext(assignment).getStudents()) {
                    int p = getProperty(s, 0);
                    value += p;
                    if (mi > p) mi = p;
                    if (mx < p) mx = p;
                    nrStudents ++;
                }
                double cnt = value / nrStudents;
                if (cnt < min) min = cnt;
                if (cnt > max) max = cnt;
                total += cnt;
                teams ++;
                info.put(getKey() + " [" + t.getName() + "]", sDoubleFormat.format(cnt) + " (" + mi + ".." + mx + ")"); 
            }
        }
        double avg = total / teams;
        double rms = 0;
        for (Constraint<Student, TeamAssignment> c: getModel().constraints()) {
            if (c instanceof Team) {
                Team t = (Team)c;
                int nrStudents = 0;
                double value = 0.0;
                for (Student s: t.getContext(assignment).getStudents()) {
                    value += getProperty(s, 0);
                    nrStudents ++;
                }
                double cnt = value / nrStudents;
                rms += Math.pow(cnt - avg, 2.0);
            }
        }
        info.put(getKey() + " [Average]", sDoubleFormat.format(avg) + " +/- " + sDoubleFormat.format(Math.sqrt(rms / teams)) + " (" + sDoubleFormat.format(min) + ".." + sDoubleFormat.format(max) + ")");
    }

}
