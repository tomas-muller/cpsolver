package org.cpsolver.bgr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.heuristics.NeighbourSelection;
import org.cpsolver.ifs.model.Neighbour;
import org.cpsolver.ifs.model.SimpleNeighbour;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;

public class ConstructionHeuristics implements NeighbourSelection<Student, TeamAssignment> {
    private Iterator<Student> iStudents = null;
    
    public ConstructionHeuristics(DataProperties config) {
    }
    
    @Override
    public void init(Solver<Student, TeamAssignment> solver) {
        List<Student> students = new ArrayList<Student>(solver.currentSolution().getModel().variables());
        Collections.sort(students, new Comparator<Student>() {
            @Override
            public int compare(Student s1, Student s2) {
                // international students first
                if (s1.isInternational() != s2.isInternational())
                    return s1.isInternational() ? -1 : 1;
                return s1.getProperty("PUID").compareTo(s2.getProperty("PUID"));
            }
        });
        iStudents = students.iterator();
    }

    @Override
    public Neighbour<Student, TeamAssignment> selectNeighbour(Solution<Student, TeamAssignment> solution) {
        TeamBuilder model = (TeamBuilder)solution.getModel();
        Assignment<Student, TeamAssignment> assignment = solution.getAssignment();
        
        if (iStudents == null || !iStudents.hasNext()) return null;
        
        Student student = iStudents.next();
        
        TeamAssignment best = null; double bestVal = 0.0;
        for (Team team: model.getTeams()) {
            String rhg = team.getTeamLead().getProperty("BGRHallGroup");
            if (rhg == null && team.getContext(assignment).getStudents().isEmpty()) continue;
            TeamAssignment ta = new TeamAssignment(student, team);
            if (model.inConflict(assignment, ta)) continue;
            // 100000.0 * team.getContext(assignment).getStudents().size() + 
            double value = ta.toDouble(assignment);
            if (best == null || value < bestVal) { best = ta; bestVal = value; }
        }
        if (best == null) {
            for (Team team: model.getTeams()) {
                TeamAssignment ta = new TeamAssignment(student, team);
                if (model.inConflict(assignment, ta)) continue;
                double value = ta.toDouble(assignment);
                if (best == null || value < bestVal) { best = ta; bestVal = value; }
            }
        }
        if (best == null) {
            String rhg = student.getProperty("BGRHallGroup");
            String rd = student.getProperty("RetreatDate");
            System.err.println("Failed to assign " + student.getProperty("PUID") + " from " + (rhg == null ? "NA" : rhg) + (rd == null ? "" : " and retreat date " + rd));
            return new SimpleNeighbour<Student, TeamAssignment>(student, null);
        }
        
        return new SimpleNeighbour<Student, TeamAssignment>(best.variable(), best);
    }

}
