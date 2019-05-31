package org.cpsolver.teams;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private Map<String, Criterion<Student, TeamAssignment>> iFeatures = new HashMap<String, Criterion<Student, TeamAssignment>>();
    
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
    
    @Override
    public void addCriterion(Criterion<Student, TeamAssignment> criterion) {
        if (criterion instanceof Feature) {
            iFeatures.put(((Feature)criterion).getName(), criterion);
            criterion.setModel(this);
            addModelListener(criterion);
        } else {
            super.addCriterion(criterion);
        }
    }
    
    @Override
    public void removeCriterion(Criterion<Student, TeamAssignment> criterion) {
        if (criterion instanceof Feature) {
            iFeatures.remove(((Feature)criterion).getName());
            criterion.setModel(null);
            removeModelListener(criterion);
        } else {
            super.removeCriterion(criterion);
        }
    }
    
    public Feature getFeature(String name) {
        return (Feature)iFeatures.get(name);
    }
    
    @Override
    public Collection<Criterion<Student, TeamAssignment>> getCriteria() {
        if (super.getCriteria().isEmpty()) {
            return iFeatures.values();
        } else {
            List<Criterion<Student, TeamAssignment>> criteria = new ArrayList<Criterion<Student, TeamAssignment>>(iFeatures.values());
            criteria.addAll(super.getCriteria());
            return criteria;
        }
    }
    
    public static class ShutdownHook extends Thread {
        Solver<Student, TeamAssignment> iSolver = null;

        public ShutdownHook(Solver<Student, TeamAssignment> solver) {
            setName("ShutdownHook");
            iSolver = solver;
        }

        @Override
        public void run() {
            try {
                if (iSolver.isRunning())
                    iSolver.stopSolver();
                
                Solution<Student, TeamAssignment> solution = iSolver.lastSolution();
                solution.restoreBest();

                sLog.info("Best solution found after " + solution.getBestTime() + " seconds (" + solution.getBestIteration() + " iterations).");
                sLog.info("Number of assigned variables is " + solution.getModel().assignedVariables(solution.getAssignment()).size());
                sLog.info("Total value of the solution is " + solution.getModel().getTotalValue(solution.getAssignment()));

                sLog.info("Info: " + ToolBox.dict2string(solution.getExtendedInfo(), 2));
                
                CSVFile output = new CSVFile();
                List<CSVFile.CSVField> header = new ArrayList<CSVFile.CSVField>();
                header.add(new CSVFile.CSVField("Team"));
                CSVFile file = new CSVFile(new File(iSolver.getProperties().getProperty("input")));
                header.addAll(file.getHeader().getFields());
                output.setHeader(header); 
                for (Constraint<Student, TeamAssignment> c: solution.getModel().constraints()) {
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
                output.save(new File(iSolver.getProperties().getProperty("output")));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    protected void createTeams(int idx, List<SameFeature> sameFeatures, int teamSize, List<Student> students, List<Team> teams, String name, int extra) {
        if (idx == 0) {
            String line = "";
            for (SameFeature sf: sameFeatures)
                line += sf.getKey() + ",";
            sLog.info(line + "Number of Students,Lead,Other,Number of Teams");
        }
        if (idx == sameFeatures.size()) {
            double size = 0.0;
            for (Student s: students)
                size += s.getWeight();
            int nrTeams = (int)Math.ceil(size / teamSize) + extra;
            sLog.info(name + students.size() + "," + Math.round(students.size() - size) + "," + Math.round(size) + "," + nrTeams);
            for (int i = 1; i <= nrTeams; i++) {
                Team t = new Team(new Long(teams.size()), "Team " + (teams.size() + 1), teamSize);
                teams.add(t);
                for (Student s: students) t.addVariable(s);
                addConstraint(t);
            }
        } else {
            SameFeature sf = sameFeatures.get(idx);
            Map<String, List<Student>> val2students = new HashMap<String, List<Student>>();
            for (Student s: students) {
                String val = sf.getProperty(s);
                if (val == null || val.isEmpty()) val = "-";
                List<Student> stds = val2students.get(val);
                if (stds == null) {
                    stds = new ArrayList<Student>();
                    val2students.put(val, stds);
                }
                stds.add(s);
            }
            for (Map.Entry<String, List<Student>> e: val2students.entrySet()) {
                createTeams(idx + 1, sameFeatures, teamSize, e.getValue(), teams, name + "\"" + e.getKey() + "\",", extra);
            }
        }
    }
    
    public static void main(String[] args) throws IOException {
        DataProperties config = new DataProperties();
        if (System.getProperty("config") != null)
            config.load(new FileInputStream(System.getProperty("config")));
        else
            config.load(TeamBuilder.class.getClass().getResourceAsStream("/org/cpsolver/teams/teams.properties"));
        config.putAll(System.getProperties());
        ToolBox.configureLogging();
        
        TeamBuilder model = new TeamBuilder();
        CSVFile file = new CSVFile(new File(config.getProperty("input")));
        String leadAttr = config.getProperty("CSV.Lead.Attribute");
        String leadValue = config.getProperty("CSV.Lead.Value");
        for (CSVFile.CSVLine line: file.getLines()) {
            Student student = new Student();
            for (CSVFile.CSVField head: file.getHeader().getFields()) {
                CSVFile.CSVField field = line.getField(head.toString());
                if (field != null && !field.toString().isEmpty())
                    student.setProperty(head.toString(), field.toString());
            }
            if (leadValue != null && leadValue.equals(student.getProperty(leadAttr))) student.setWeight(0);
            model.addVariable(student);
        }
        
        List<SameFeature> sameFeatures = new ArrayList<SameFeature>();
        Set<String> weakerSameFeatures = new HashSet<String>();
        for (String criterion: config.getProperty("Teams.Criteria").split("\\|")) {
            Feature feature = null;
            if (criterion.equals("TeamSize") || criterion.equals("Team Size")) {
                model.addCriterion(new TeamSize());
                sLog.info("Using Team Size with weight of " + config.getPropertyDouble("Weight.TeamSize", 1.0));
                continue;
            } else if (criterion.startsWith("@"))
                feature = new IntegerFeature(criterion.substring(1).split(","));
            else if (criterion.startsWith("^"))
                feature = new ReversedFeature(criterion.substring(1).split(","));
            else if (criterion.startsWith("#"))
                feature = new RoomNearby(criterion.substring(1).split(","));
            else if (criterion.startsWith("!")) {
                SameFeature sf = new SameFeature(criterion.substring(1).split(","));
                sLog.info("Using " + sf.getKey() + " same feature constraint.");
                sameFeatures.add(sf);
                model.addGlobalConstraint(sf);
                continue;
            } else if (criterion.startsWith("?")) {
                SameFeature sf = new SameFeature(criterion.substring(1).split(","));
                sLog.info("Using " + sf.getKey() + " same feature constraint (not for leads).");
                sameFeatures.add(sf);
                weakerSameFeatures.add(sf.getKey());
                // model.addGlobalConstraint(sf);
                continue;
            } else
                feature = new Feature(criterion.split(","));
            sLog.info("Using " + feature.getName() + " with weight of " + config.getPropertyDouble(feature.getWeightName(), feature.getWeightDefault(config)) + " (" + (feature instanceof IntegerFeature ? "integer feature" : "text feature") + ")");
            model.addCriterion(feature);
        }
        
        int teamSize = config.getPropertyInt("teamSize", 5);
        if (sameFeatures.isEmpty()) {
            int nrTeams = (int)Math.ceil(((double)model.variables().size()) / teamSize) + config.getPropertyInt("extraTeams", 0);
            for (int i = 1; i <= nrTeams; i++) {
                Team t = new Team(new Long(i), "Team " + i, teamSize);
                for (Student s: model.variables()) t.addVariable(s);
                model.addConstraint(t);
            }
        } else {
            List<Team> teams = new ArrayList<Team>();
            model.createTeams(0, sameFeatures, teamSize, model.variables(), teams, "", config.getPropertyInt("extraTeams", 0));
            if (!weakerSameFeatures.isEmpty()) {
                for (Student s: model.variables()) {
                    if (s.getWeight() == 0) {
                        for (Team t: teams) {
                            Student x = t.variables().get(0);
                            boolean match = true;
                            for (SameFeature sf: sameFeatures) {
                                if (!weakerSameFeatures.contains(sf.getKey()) && !sf.same(s, x)) match = false;
                                if (weakerSameFeatures.contains(sf.getKey()) && sf.same(s, x)) match = false;
                            }
                            if (match) t.addVariable(s);
                        }
                    }
                }   
            }
        }
        
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

        Runtime.getRuntime().addShutdownHook(new ShutdownHook(solver));
        
        solver.start();
        try {
            solver.getSolverThread().join();
        } catch (InterruptedException e) {
        }
    }
}
