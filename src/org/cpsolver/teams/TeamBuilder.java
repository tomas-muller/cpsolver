package org.cpsolver.teams;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.DefaultParallelAssignment;
import org.cpsolver.ifs.assignment.DefaultSingleAssignment;
import org.cpsolver.ifs.criteria.Criterion;
import org.cpsolver.ifs.model.Constraint;
import org.cpsolver.ifs.model.Model;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solution.SolutionListener;
import org.cpsolver.ifs.solver.ParallelSolver;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.CSVFile;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.ToolBox;

public class TeamBuilder extends Model<Student, TeamAssignment>{
    private static Logger sLog = Logger.getLogger(TeamBuilder.class);
    
    @Override
    public double getTotalValue(Assignment<Student, TeamAssignment> assignment) {
        double ret = 0;
        for (Criterion<Student, TeamAssignment> criterion : getCriteria())
            ret += criterion.getWeightedValue(assignment);
        return ret;
    }

    @Override
    public double getTotalValue(Assignment<Student, TeamAssignment> assignment, Collection<Student> variables) {
        double ret = 0;
        for (Criterion<Student, TeamAssignment> criterion : getCriteria())
            ret += criterion.getWeightedValue(assignment, variables);
        return ret;
    }
    
    public static void main(String[] args) throws IOException {
        DataProperties config = new DataProperties();
        config.load(TeamBuilder.class.getClass().getResourceAsStream("/org/cpsolver/teams/teams.properties"));
        config.putAll(System.getProperties());
        ToolBox.configureLogging();
        
        TeamBuilder model = new TeamBuilder();
        CSVFile file = new CSVFile(new File(config.getProperty("input")));
        for (CSVFile.CSVLine line: file.getLines()) {
            Student student = new Student();
            for (CSVFile.CSVField head: file.getHeader().getFields()) {
                CSVFile.CSVField field = line.getField(head.toString());
                if (field != null && !field.toString().isEmpty())
                    student.setProperty(head.toString(), field.toString());
            }
            model.addVariable(student);
        }
        
        int teamSize = config.getPropertyInt("teamSize", 5);
        int nrTeams = (int)Math.ceil(((double)model.variables().size()) / teamSize);
        for (int i = 1; i <= nrTeams; i++) {
            Team t = new Team(new Long(i), "Team " + i, teamSize);
            for (Student s: model.variables()) t.addVariable(s);
            model.addConstraint(t);
        }
        
        model.addCriterion(new Feature("Program"){});
        model.addCriterion(new Feature("Gender"){});
        model.addCriterion(new IntegerFeature("GMAT", "Predicted GMAT"){});
        model.addCriterion(new IntegerFeature("MW-Post"){});
        model.addCriterion(new Feature("Major"){});
        model.addCriterion(new Feature("Inst"){});
        model.addCriterion(new Feature("Country of Citizenship"){});
        
        int nrSolvers = config.getPropertyInt("Parallel.NrSolvers", 1);
        Solver<Student, TeamAssignment> solver = (nrSolvers == 1 ? new Solver<Student, TeamAssignment>(config) : new ParallelSolver<Student, TeamAssignment>(config));
        
        Assignment<Student, TeamAssignment> assignment = (nrSolvers <= 1 ? new DefaultSingleAssignment<Student, TeamAssignment>() : new DefaultParallelAssignment<Student, TeamAssignment>());
        
        solver.setInitalSolution(new Solution<Student, TeamAssignment>(model, assignment));

        solver.currentSolution().addSolutionListener(new SolutionListener<Student, TeamAssignment>() {
            @Override
            public void solutionUpdated(Solution<Student, TeamAssignment> solution) {
            }

            @Override
            public void getInfo(Solution<Student, TeamAssignment> solution, Map<String, String> info) {
            }

            @Override
            public void getInfo(Solution<Student, TeamAssignment> solution, Map<String, String> info, Collection<Student> variables) {
            }

            @Override
            public void bestCleared(Solution<Student, TeamAssignment> solution) {
            }

            @Override
            public void bestSaved(Solution<Student, TeamAssignment> solution) {
                Model<Student, TeamAssignment> m = solution.getModel();
                Assignment<Student, TeamAssignment> a = solution.getAssignment();
                System.out.println("**BEST[" + solution.getIteration() + "]** " + m.toString(a));
            }

            @Override
            public void bestRestored(Solution<Student, TeamAssignment> solution) {
            }
        });

        solver.start();
        try {
            solver.getSolverThread().join();
        } catch (InterruptedException e) {
        }
        
        Solution<Student, TeamAssignment> solution = solver.lastSolution();
        solution.restoreBest();

        sLog.info("Best solution found after " + solution.getBestTime() + " seconds (" + solution.getBestIteration() + " iterations).");
        sLog.info("Number of assigned variables is " + solution.getModel().assignedVariables(solution.getAssignment()).size());
        sLog.info("Total value of the solution is " + solution.getModel().getTotalValue(solution.getAssignment()));

        sLog.info("Info: " + ToolBox.dict2string(solution.getExtendedInfo(), 2));
        
        CSVFile output = new CSVFile();
        List<CSVFile.CSVField> header = new ArrayList<CSVFile.CSVField>();
        header.add(new CSVFile.CSVField("Team"));
        header.addAll(file.getHeader().getFields());
        output.setHeader(header); 
        for (Constraint<Student, TeamAssignment> c: model.constraints()) {
            if (c instanceof Team) {
                Team t = (Team)c;
                for (Student s: t.getContext(solution.getAssignment()).getStudents()) {
                    List<CSVFile.CSVField> line = new ArrayList<CSVFile.CSVField>();
                    line.add(new CSVFile.CSVField(t.getName()));
                    for (CSVFile.CSVField head: file.getHeader().getFields()) {
                        line.add(new CSVFile.CSVField(s.getProperty(head.toString())));
                    }
                    output.addLine(line);
                }
            }
        }
        output.save(new File(config.getProperty("output")));
    }
}
