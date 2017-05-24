package org.cpsolver.bgr;

import java.util.HashMap;
import java.util.Map;

import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.heuristics.NeighbourSelection;
import org.cpsolver.ifs.model.LazyNeighbour;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.ifs.model.Neighbour;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.ToolBox;

public class MoveAway implements NeighbourSelection<Student, TeamAssignment> {
    
    public MoveAway(DataProperties config) {
    }

    @Override
    public void init(Solver<Student, TeamAssignment> solver) {
    }

    @Override
    public Neighbour<Student, TeamAssignment> selectNeighbour(Solution<Student, TeamAssignment> solution) {
        TeamBuilder model = (TeamBuilder)solution.getModel();
        Assignment<Student, TeamAssignment> assignment = solution.getAssignment();
        int r1 = ToolBox.random(model.variables().size());
        for (int i = 0; i < model.variables().size(); i++) {
            Student s = model.variables().get((i + r1) % model.variables().size());
            TeamAssignment a = assignment.getValue(s);
            if (a == null) continue;
            int r2 = ToolBox.random(model.getTeams().size());
            for (int j = 0; j < model.variables().size(); j++) {
                Team t = model.getTeams().get((j + r2) % model.getTeams().size());
                if (a.getTeam().equals(t)) continue;
                TeamAssignment n = new TeamAssignment(s, t);
                if (!model.inConflict(assignment, n))
                    return new Move(a, n);
            }
        }
        return null;
    }
    
    public static class Move extends LazyNeighbour<Student, TeamAssignment> {
        private TeamAssignment iOld, iNew;
        
        public Move(TeamAssignment oldAssignment, TeamAssignment newAssignment) {
            iOld = oldAssignment; iNew = newAssignment;
        }

        @Override
        public Map<Student, TeamAssignment> assignments() {
            Map<Student, TeamAssignment> map = new HashMap<Student, TeamAssignment>();
            map.put(iNew.variable(), iNew);
            return map;
        }

        @Override
        protected void doAssign(Assignment<Student, TeamAssignment> assignment, long iteration) {
            assignment.assign(iteration, iNew);
        }

        @Override
        protected void undoAssign(Assignment<Student, TeamAssignment> assignment, long iteration) {
            assignment.assign(iteration, iOld);
        }

        @Override
        public Model<Student, TeamAssignment> getModel() {
            return iNew.variable().getModel();
        }
    }

}
