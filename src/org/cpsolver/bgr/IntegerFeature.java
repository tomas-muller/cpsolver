package org.cpsolver.bgr;

import org.cpsolver.ifs.solver.Solver;

public class IntegerFeature extends Feature {
    public int iMaximum = 0;
    
    IntegerFeature(String key, String... fallbacks) {
        super(key, fallbacks);
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

}
