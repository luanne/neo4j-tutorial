package org.neo4j.tutorial;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.neo4j.tutorial.matchers.ContainsOnlyHumanCompanions.containsOnlyHumanCompanions;
import static org.neo4j.tutorial.matchers.ContainsOnlySpecificTitles.containsOnly;

import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;

/**
 * In this Koan we start to mix indexing and core API to perform more targeted
 * graph operations. We'll mix indexes and core graph operations to explore the
 * Doctor's universe.
 */
public class Koan04 {

    private DoctorWhoUniverse universe;

    @Before
    public void createADatabase() {

        universe = new DoctorWhoUniverse();
    }

    @Test
    public void shouldCountTheNumberOfDoctorsRegenerations() {

        Index<Node> actorsIndex = universe.getDatabase().index().forNodes("actors");
        int numberOfRegenerations = 1;

        // SNIPPET_START
        Node firstDoctor = actorsIndex.get("lastname", "Hartnell").getSingle();

        Relationship regeneratedTo = firstDoctor.getSingleRelationship(DoctorWhoUniverse.REGENERATED_TO, Direction.OUTGOING);

        while (regeneratedTo != null) {
            numberOfRegenerations++;
            regeneratedTo = regeneratedTo.getEndNode().getSingleRelationship(DoctorWhoUniverse.REGENERATED_TO, Direction.OUTGOING);
        }

        // SNIPPET_END

        assertEquals(11, numberOfRegenerations);
    }

    @Test
    public void shouldFindHumanCompanionsUsingCoreApi() {
        IndexHits<Node> companions = null;

        // SNIPPET_START

        companions = universe.getDatabase().index().forNodes("companions").query("name", "*");

        // SNIPPET_END

        HashSet<Node> humanCompanions = new HashSet<Node>();

        // SNIPPET_START

        for (Node n : companions) {

            if (n.hasRelationship(DoctorWhoUniverse.IS_A, Direction.OUTGOING)) {
                Relationship relationship = n.getSingleRelationship(DoctorWhoUniverse.IS_A, Direction.OUTGOING);
                if (relationship.getEndNode().getProperty("species").equals("Human")) {
                    humanCompanions.add(n);
                }
            }
        }

        // SNIPPET_END

        int numberOfKnownHumanCompanions = 35;
        assertEquals(numberOfKnownHumanCompanions, humanCompanions.size());
        assertThat(humanCompanions, containsOnlyHumanCompanions());
    }

    @Test
    public void shouldFindAllEpisodesWhereRoseTylerFoughtTheDaleks() {
        Index<Node> friendliesIndex = universe.getDatabase().index().forNodes("friendlies");
        Index<Node> speciesIndex = universe.getDatabase().index().forNodes("species");
        HashSet<Node> episodesWhereRoseFightsTheDaleks = new HashSet<Node>();

        // SNIPPET_START

        Node roseTyler = friendliesIndex.get("name", "Rose Tyler").getSingle();
        Node daleks = speciesIndex.get("species", "Dalek").getSingle();

        for (Relationship r1 : roseTyler.getRelationships(DoctorWhoUniverse.APPEARED_IN, Direction.OUTGOING)) {
            Node episode = r1.getEndNode();

            for (Relationship r2 : episode.getRelationships(DoctorWhoUniverse.APPEARED_IN, Direction.INCOMING)) {
                if (r2.getStartNode().equals(daleks)) {
                    episodesWhereRoseFightsTheDaleks.add(episode);
                }
            }
        }

        // SNIPPET_END

        assertThat(episodesWhereRoseFightsTheDaleks,
                containsOnly("Army of Ghosts", "The Stolen Earth", "Doomsday", "Journey's End", "Bad Wolf", "The Parting of the Ways", "Dalek"));
    }
}
