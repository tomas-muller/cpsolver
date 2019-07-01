package org.cpsolver.teams;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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

import org.cpsolver.ifs.util.CSVFile.CSVField;
import org.cpsolver.ifs.util.CSVFile.CSVLine;

public class PYTSmallTeamBuilder {
    private static Logger sLog = Logger.getLogger(PYTSmallTeamBuilder.class);
    
    public static void main(String[] args) throws IOException {
        DataProperties config = new DataProperties();
        if (System.getProperty("config") != null)
            config.load(new FileInputStream(System.getProperty("config")));
        else
            config.load(PYTSmallTeamBuilder.class.getClass().getResourceAsStream("/org/cpsolver/teams/pyt.properties"));
        config.putAll(System.getProperties());
        ToolBox.configureLogging();
        boolean roomRegExp = config.getPropertyBoolean("CSV.roomRestrictionsRegExp", false);
        
        List<Group> groups = new ArrayList<Group>();
        for (String group: config.getProperty("groups").split(","))
            groups.add(new Group(group.split(":")[0],group.split(":")[1]));
        
        CSVFile roomFile = new CSVFile(new File(config.getProperty("rooms")));
        List<Room> rooms = new ArrayList<Room>();
        for (CSVFile.CSVLine line: roomFile.getLines()) {
            if (line.getField(0) == null || line.getField(0).isEmpty()) continue;
            rooms.add(new Room(line.getField(config.getProperty("CSV.roomName")).toString(), line.getField(config.getProperty("CSV.roomCapacity")).toInt()));
        }
        
        CSVFile attendeesFile = new CSVFile(new File(config.getProperty("attendees")));
        List<Delegation> ungrouped = new ArrayList<Delegation>();
        List<Student> leaders = new ArrayList<Student>();
        Map<String, Delegation> delegations = new HashMap<String, Delegation>();
        for (CSVFile.CSVLine line: attendeesFile.getLines()) {
            if (line.getField(0) == null || line.getField(0).isEmpty()) continue;
            CSVField attendeeId = line.getField(config.getProperty("CSV.attendeeId"));
            CSVField delegationId = line.getField(config.getProperty("CSV.delegationId"));
            CSVField delegationName = line.getField(config.getProperty("CSV.delegationName"));
            if (attendeeId == null || delegationId == null) continue;
            
            CSVField leader = line.getField(config.getProperty("CSV.leaderColumn"));
            if (leader != null && leader.toString().equals(config.getProperty("CSV.leaderValue"))) {
                Student student = new Student();
                for (CSVFile.CSVField head: attendeesFile.getHeader().getFields()) {
                    CSVFile.CSVField field = line.getField(head.toString());
                    if (field != null && !field.toString().isEmpty())
                        student.setProperty(head.toString(), field.toString());
                }
                student.setLeader(true);
                leaders.add(student);
                continue;
            }
            
            CSVField groupId = line.getField(config.getProperty("CSV.groupDesignator"));
            Group group = null;
            if (groupId != null && !groupId.isEmpty()) {
                for (Group g: groups)
                    if (g.getDesignation().equals(groupId.toString())) { group = g; break; }
            }
            Delegation delegation = delegations.get(delegationId.toString());
            if (delegation == null) {
                delegation = new Delegation(delegationId.toString(), delegationName.toString(), group);
                delegations.put(delegationId.toString(), delegation);
                if (group == null)
                    ungrouped.add(delegation);
                else
                    group.getDelegations().add(delegation);
            }
            delegation.getAttendees().add(new Attendee(line));
        }
        
        if (!ungrouped.isEmpty()) {
            Collections.sort(ungrouped);
            for (Delegation delegation: ungrouped) {
                Group group = null;
                for (Group g: groups) {
                    if (group == null || g.size() < group.size()) group = g;
                }
                delegation.setGroup(group);
                group.getDelegations().add(delegation);
            }
        }
        
        Collections.sort(rooms);
        for (Group group: groups) {
            Iterator<Room> i = rooms.iterator();
            Collections.sort(group.getDelegations());
            for (Delegation delegation: group.getDelegations()) {
                if (delegation.size() > config.getPropertyInt("Delegation.MinRoomSize", 20)) {
                    Room room = i.next();
                    delegation.setRoom(room);
                }
            }
        }
        
        CSVFile delegationsFile = new CSVFile();
        List<CSVFile.CSVField> delegationsHeader = new ArrayList<CSVFile.CSVField>();
        delegationsHeader.add(new CSVFile.CSVField(config.get("CSV.delegationId")));
        delegationsHeader.add(new CSVFile.CSVField(config.get("CSV.delegationName")));
        delegationsHeader.add(new CSVFile.CSVField("Participants"));
        delegationsHeader.add(new CSVFile.CSVField(config.get("CSV.groupDesignator")));
        delegationsHeader.add(new CSVFile.CSVField(config.get("CSV.roomName")));
        delegationsHeader.add(new CSVFile.CSVField(config.get("CSV.roomCapacity")));
        delegationsFile.setHeader(delegationsHeader); 
        for (Group group: groups) {
            for (Delegation delegation: group.getDelegations()) {
                List<CSVField> line = new ArrayList<CSVFile.CSVField>();
                line.add(new CSVFile.CSVField(delegation.getDelegationId()));
                line.add(new CSVFile.CSVField(delegation.getDelegationName()));
                line.add(new CSVFile.CSVField(delegation.size()));
                line.add(new CSVFile.CSVField(delegation.getGroup().getDesignation()));
                line.add(new CSVFile.CSVField(delegation.getRoom() == null ? null : delegation.getRoom().getName()));
                line.add(new CSVFile.CSVField(delegation.getRoom() == null ? null : delegation.getRoom().getCapacity()));
                delegationsFile.addLine(line);
            }
        }
        delegationsFile.save(new File(config.getProperty("delegations")));
        
        CSVFile teamsFile = new CSVFile();
        List<CSVFile.CSVField> teamsHeader = new ArrayList<CSVFile.CSVField>();
        teamsHeader.add(new CSVFile.CSVField("Team"));
        teamsHeader.add(new CSVFile.CSVField(config.get("CSV.roomName")));
        teamsHeader.add(new CSVFile.CSVField(config.get("CSV.roomCapacity")));
        teamsHeader.addAll(attendeesFile.getHeader().getFields());
        teamsFile.setHeader(teamsHeader); 
        
        CSVFile statsFile = new CSVFile();
        List<CSVFile.CSVField> statsHeader = new ArrayList<CSVFile.CSVField>();
        statsHeader.add(new CSVFile.CSVField("Team"));
        statsHeader.add(new CSVFile.CSVField("Attendees"));
        statsHeader.add(new CSVFile.CSVField(config.get("CSV.roomName")));
        statsHeader.add(new CSVFile.CSVField(config.get("CSV.roomCapacity")));
        Set<String> properties = new TreeSet<String>();
        for (String criterion: config.getProperty("Teams.Criteria").split("\\|")) {
            Feature feature = null;
            if (criterion.startsWith("@"))
                feature = new IntegerFeature(criterion.substring(1).split(","));
            else if (criterion.startsWith("%"))
                feature = new ProportionalFeature(criterion.substring(1).split(","));
            else
                feature = new Feature(criterion.split(","));
            for (Group group: groups)
                for (Delegation delegation: group.getDelegations())
                    for (Attendee attendee: delegation.getAttendees()) {
                        String value = attendee.getProperty(feature);
                        if (value != null) properties.add(feature.getKey() + ": " + value);
                    }
            for (Student student: leaders) {
                String value = feature.getProperty(student);
                if (value != null) properties.add(feature.getKey() + ": " + value);
            }
        }
        for (String value: properties)
            statsHeader.add(new CSVFile.CSVField(value));
        statsFile.setHeader(statsHeader);
        
        // assign group designation to undesigned team leaders with same team association
        leaders: for (Student leader: leaders) {
            if (leader.getProperty(config.getProperty("CSV.groupDesignator")) == null) {
                for (Group group: groups)
                    for (Delegation delegation: group.getDelegations())
                        for (Attendee attendee: delegation.getAttendees()) {
                            String otherId = attendee.getProperty(config.getProperty("CSV.sameAsId"));
                            if (otherId != null && otherId.equals(leader.getProperty(config.getProperty("CSV.attendeeId")))) {
                                leader.setProperty(config.getProperty("CSV.groupDesignator"), group.getDesignation());
                                continue leaders;
                            }
                        }
            }
        }
        
        for (Group group: groups) {
            TeamBuilder model = new TeamBuilder();
            
            List<Student> students = new ArrayList<Student>();
            for (Delegation delegation: group.getDelegations())
                for (Attendee attendee: delegation.getAttendees()) {
                    Student student = new Student();
                    for (CSVFile.CSVField head: attendeesFile.getHeader().getFields()) {
                        CSVFile.CSVField field = attendee.getLine().getField(head.toString());
                        if (field != null && !field.toString().isEmpty())
                            student.setProperty(head.toString(), field.toString());
                    }
                    model.addVariable(student);
                    students.add(student);
                }
            
            Map<Team, Room> team2room = new HashMap<Team, Room>();
            Group pair = null;
            for (Group g: groups)
                if (g.getPairWith().equals(group.getDesignation())) pair = g;

            int maxTeams = Math.min((int)Math.ceil(((double)group.size()) / config.getPropertyInt("Group.DesiredSize", 1)), config.getPropertyInt("Group.MaxTeams", leaders.size() / groups.size()));
            List<Team> teams = new ArrayList<Team>();
            
            int teamId = 1;
            for (Room room: rooms) {
                Delegation taken = null;
                if (pair != null) 
                    for (Delegation d: pair.getDelegations())
                        if (room.equals(d.getRoom())) { taken = d; break; }
                if (taken != null) continue;
                Team t = new Team(new Long(teamId), group.getDesignation() + "-" + teamId, Math.min(config.getPropertyInt("Group.MaxSize", 25), room.getCapacity() - config.getPropertyInt("Group.MinFreeSpace", 4))); 
                teamId ++;
                for (Student s: model.variables()) {
                    String restrictions = s.getProperty(config.getProperty("CSV.roomRestrictions"));
                    if (restrictions != null && !restrictions.isEmpty()) {
                        boolean match = false;
                        for (String r: restrictions.split("[\n\r]")) {
                            if (r.isEmpty()) continue;
                            // if ("ME 1051".equals(r)) { match = true; continue; }
                            if (room.getName().equalsIgnoreCase(r) || room.getName().startsWith(r + " ")) { match = true; break; }
                            if (roomRegExp && room.getName().matches(r)) { match = true; break; }
                        }
                        if (!match) continue;
                    }
                    t.addVariable(s);
                }
                model.addConstraint(t);
                team2room.put(t, room);
                teams.add(t);
                if (teams.size() >= maxTeams) break;
            }
            
            DifferentTeam dt = new DifferentTeam();
            // take leaders with designation first
            for (Student leader: leaders) {
                String designation = leader.getProperty(config.getProperty("CSV.groupDesignator"));
                if (designation == null || !designation.equals(group.getDesignation())) continue;
                dt.addVariable(leader);
            }
            // take leaders with no designation to fill the count
            for (Student leader: leaders) {
                if (dt.variables().size() >= teams.size()) break;
                if (leader.getProperty(config.getProperty("CSV.groupDesignator")) != null) continue;
                leader.setProperty(config.getProperty("CSV.groupDesignator"), group.getDesignation());
                dt.addVariable(leader);
            }
            if (!dt.variables().isEmpty()) {
                for (Student leader: dt.variables()) {
                    model.addVariable(leader);
                    for (Team t: teams) {
                        Room room = team2room.get(t);
                        String restrictions = leader.getProperty(config.getProperty("CSV.roomRestrictions"));
                        if (restrictions != null && !restrictions.isEmpty()) {
                            boolean match = false;
                            for (String r: restrictions.split("[\n\r]")) {
                                if (r.isEmpty()) continue;
                                if (room.getName().equalsIgnoreCase(r) || room.getName().startsWith(r + " ")) { match = true; break; }
                                if (roomRegExp && room.getName().matches(r)) { match = true; break; }
                            }
                            if (!match) continue;
                        }
                        t.addVariable(leader);
                    }
                }
                model.addConstraint(dt);
            }
            
            List<SameTeam> sameTeams = new ArrayList<SameTeam>();
            for (Student student: model.variables()) {
                String otherId = student.getProperty(config.getProperty("CSV.sameAsId"));
                Student other = null;
                if (otherId != null && !otherId.isEmpty()) {
                    for (Student s: model.variables()) {
                        if (otherId.equals(s.getProperty(config.getProperty("CSV.attendeeId")))) {
                            other = s;
                        }
                    }
                }
                if (other != null) {
                    SameTeam sameTeam = null;
                    for (SameTeam st: sameTeams) {
                        if (st.variables().contains(other)) {
                            if (!st.variables().contains(student)) {
                                st.addVariable(student);
                                student.setSameTeam(st);
                            }
                            sameTeam = st;
                            break;
                        } else if (st.variables().contains(student)) {
                            if (!st.variables().contains(other)) {
                                st.addVariable(other);
                                other.setSameTeam(st);
                            }
                            sameTeam = st;
                            break;
                        }
                    }
                    if (sameTeam == null) {
                        sameTeam = new SameTeam();
                        sameTeam.addVariable(student);
                        sameTeam.addVariable(other);
                        student.setSameTeam(sameTeam);
                        other.setSameTeam(sameTeam);
                        model.addConstraint(sameTeam);
                        sameTeams.add(sameTeam);
                    }
                }
            }
            for (SameTeam sameTeam: sameTeams) {
                sLog.info("Same team: " + sameTeam.variables());
            }
            
            for (String criterion: config.getProperty("Teams.Criteria").split("\\|")) {
                Feature feature = null;
                if (criterion.startsWith("@"))
                    feature = new IntegerFeature(criterion.substring(1).split(","));
                else if (criterion.startsWith("%"))
                    feature = new ProportionalFeature(criterion.substring(1).split(","));
                else
                    feature = new Feature(criterion.split(","));
                sLog.info("Using " + feature.getName() + " with weight of " + config.getPropertyDouble(feature.getWeightName(), feature.getWeightDefault(config)) + " (" + (feature instanceof IntegerFeature ? "integer feature" : "text feature") + ")");
                model.addCriterion(feature);
            }
            
            int nrSolvers = config.getPropertyInt("Parallel.NrSolvers", 1);
            Solver<Student, TeamAssignment> solver = (nrSolvers == 1 ? new Solver<Student, TeamAssignment>(config) : new ParallelSolver<Student, TeamAssignment>(config));
            
            Assignment<Student, TeamAssignment> assignment = (nrSolvers <= 1 ? new DefaultSingleAssignment<Student, TeamAssignment>() : new DefaultParallelAssignment<Student, TeamAssignment>());
            
            for (Student s: model.variables()) {
                List<TeamAssignment> values = s.values(assignment);
                if (values.isEmpty()) {
                    sLog.warn("Student " + s.getName() + " has no placements.");
                } else if (values.size() == 1) {
                    TeamAssignment ta = values.get(0);
                    s.setInitialAssignment(ta);
                    if (!model.inConflict(assignment, ta))
                        assignment.assign(0, ta);
                }
            }
            
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
                    sLog.info("**BEST[" + solution.getIteration() + "]** " + m.toString(a));
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
            
            for (Constraint<Student, TeamAssignment> c: solution.getModel().constraints()) {
                if (c instanceof Team) {
                    Team t = (Team)c;
                    Room r = team2room.get(t);
                    for (Student s: t.getContext(solution.getAssignment()).getStudents()) {
                        List<CSVFile.CSVField> line = new ArrayList<CSVFile.CSVField>();
                        line.add(new CSVFile.CSVField(t.getName()));
                        line.add(new CSVFile.CSVField(r.getName()));
                        line.add(new CSVFile.CSVField(r.getCapacity()));
                        for (CSVFile.CSVField head: attendeesFile.getHeader().getFields()) {
                            line.add(new CSVFile.CSVField(s.getProperty(head.toString())));
                        }
                        teamsFile.addLine(line);
                    }
                    List<CSVFile.CSVField> line = new ArrayList<CSVFile.CSVField>();
                    line.add(new CSVFile.CSVField(t.getName()));
                    line.add(new CSVFile.CSVField(t.getContext(solution.getAssignment()).getStudents().size()));
                    line.add(new CSVFile.CSVField(r.getName()));
                    line.add(new CSVFile.CSVField(r.getCapacity()));
                    Map<String, Integer> values = new HashMap<String, Integer>();
                    for (Criterion<Student, TeamAssignment> crit: model.getCriteria()) {
                        if (crit instanceof Feature) {
                            Feature feature = (Feature)crit;
                            for (Student s: t.getContext(solution.getAssignment()).getStudents()) {
                                String value = feature.getProperty(s);
                                if (value != null) {
                                    Integer count = values.get(feature.getKey() + ": " + value);
                                    values.put(feature.getKey() + ": " + value, 1 + (count == null ? 0 : count.intValue()));
                                }
                            }
                        }
                    }
                    for (String prop: properties)
                        line.add(new CSVFile.CSVField(values.get(prop)));
                    statsFile.addLine(line);
                }
            }
        }
        
        teamsFile.save(new File(config.getProperty("teams")));
        statsFile.save(new File(config.getProperty("stats")));
        
    }
    
    private static class Room implements Comparable<Room>{
        String iName;
        int iCapacity;
        
        private Room(String name, int capacity) {
            iName = name; iCapacity = capacity;
        }
        
        public String getName() { return iName; }
        public int getCapacity() { return iCapacity; }
        
        @Override
        public String toString() { return iName; }

        @Override
        public int compareTo(Room r) {
            if (getCapacity() != r.getCapacity())
                return getCapacity() > r.getCapacity() ? -1 : 1;
            return getName().compareToIgnoreCase(r.getName());
        }
        
        @Override
        public boolean equals(Object o) {
            if (o == null || !(o instanceof Room)) return false;
            return getName().equals(((Room)o).getName());
        }
        
        @Override
        public int hashCode() {
            return getName().hashCode();
        }
    }
    
    private static class Attendee {
        CSVLine iLine;
        
        private Attendee(CSVLine line) {
            iLine = line;
        }
        
        public CSVLine getLine() { return iLine; }
        
        public String getProperty(String key) {
            if (key == null) return null;
            CSVField field = getLine().getField(key);
            if (field != null && !field.isEmpty()) return field.toString();
            return null;
        }
        
        public String getProperty(Feature feature) {
            CSVField field = getLine().getField(feature.getKey());
            if (field != null && !field.isEmpty()) return field.toString();
            if (feature.getFallbacks() != null)
                for (String fallback: feature.getFallbacks()) {
                    field = getLine().getField(fallback);
                    if (field != null && !field.isEmpty()) return field.toString();
                }
            return null;
        }
    }
    
    private static class Delegation implements Comparable<Delegation> {
        String iDelegationId, iDelegationName;
        Group iGroup;
        List<Attendee> iAttendees = new ArrayList<Attendee>();
        Room iRoom;
        
        public Delegation(String delegationId, String delegationName, Group group) {
            iDelegationId = delegationId;
            iDelegationName = delegationName;
            iGroup = group;            
        }

        public String getDelegationId() { return iDelegationId; }
        public String getDelegationName() { return iDelegationName; }
        public Group getGroup() { return iGroup; }
        public void setGroup(Group group) { iGroup = group; }
        public List<Attendee> getAttendees() { return iAttendees; }
        public int size() { return iAttendees.size(); }
        public Room getRoom() { return iRoom; }
        public void setRoom(Room room) { iRoom = room; }
        
        @Override
        public int compareTo(Delegation d) {
            if (size() != d.size())
                return size() > d.size() ? -1 : 1;
            if (!getDelegationName().equalsIgnoreCase(d.getDelegationName()))
                return getDelegationName().compareToIgnoreCase(d.getDelegationName());
            return getDelegationId().compareTo(d.getDelegationId());
        }
    }
    
    private static class Group {
        String iDesignation, iPairWith;
        List<Delegation> iDelegations = new ArrayList<Delegation>();
        
        public Group(String designation, String pairWith) { iDesignation = designation; iPairWith = pairWith; }
        
        public String getDesignation() { return iDesignation; }
        public String getPairWith() { return iPairWith; }
        public List<Delegation> getDelegations() { return iDelegations; }
        public int size() {
            int size = 0;
            for (Delegation delegation: iDelegations)
                size += delegation.getAttendees().size();
            return size;
        }
    }

}
