package neuron.procedures;

import org.eclipse.jetty.util.Index;
import org.neo4j.graphdb.*;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.stream.Stream;
import java.util.Map;

public class CreateOutputsRow {

    /* @Context to access Log Service
       and GraphDatabaseService from Neo4j
    */
    @Context
    public Log log;
    @Context
    public GraphDatabaseService db;

    // Creating Procedure <nn.createOutputsRow>

    @Procedure(name = "nn.createOutputsRow", mode = Mode.WRITE)
    @Description("Create Outputs Rows for NeuralNetwork")

    public Stream<CreateResult> createOutputsRow(@Name("id") String id) {

        try (Transaction tx = db.beginTx()) {

            String creating_query = """
                                        CREATE (n:Row {id: $id,
                                                       type: 'outputsRow'})
                                        RETURN n
                                    """;

            Result query_result = tx.execute(creating_query, Map.of("id",id)
                                            );
            Stream<CreateResult> streamout = query_result.stream()
                                                         .map(x->new CreateResult(x.get("n").toString(),
                                                                                                    null)
                                                              );
            tx.commit();

            log.info(String.format("New Row with id: %s was created ! ",
                    id));

            return streamout;

        } catch (Exception e) {

            log.error("Error creating Row:" + e.getMessage());

            return Stream.of(new CreateResult("ko" + e.getMessage(), null));
        }
    }

    // Creating Procedure <nn.connexionOutputsNeurons>

    @Procedure(name = "nn.connexionOutputsNeurons", mode = Mode.WRITE)
    @Description("Create connection between Outputs and Neurons")

    public Stream<CreateResult> ConnexionStream(@Name("from_id") String from_id,
                                                @Name("to_id") String to_id,
                                                @Name("value") Double value,
                                                @Name("outputbyrowid") String outputbyrowid) {

        if (value == null) { value = 0.0; // Assigning default value 0.0
                            }
        try (Transaction tx = db.beginTx()) {

            String connexion_query = """
                                         MATCH (n1:Neuron {{id: $from_id,type:'output'}})
                                         MATCH (n2:Row {{id: $to_id,type:'outputsRow'}})
                                         CREATE (n1)-[:CONTAINS {output: $value,id:$outputbyrowid}]->(n2)
                                         RETURN n1, n2
                                     """;

            Result query_result = tx.execute(connexion_query, Map.of("from_id", from_id,
                                               "to_id", to_id,
                                               "value", value,
                                               "outputbyrowid", outputbyrowid)
                                            );
            Stream<CreateResult> streamout = query_result.stream()
                                                         .map(x->new CreateResult(x.get("n1").toString(),
                                                                                                    x.get("n2").toString()
                                                                                                    )
                                                              );

            tx.commit();

            log.info(String.format("New Connexion successfuly established " +
                    "from Neuron: %s to Output: %s",from_id,to_id));

            return streamout;

        } catch (Exception e) {

            log.error("Error creating Connexion:" + e.getMessage());

            return Stream.of(new CreateResult("Failure :(", null));
        }
    }

    public static class CreateResult {

        public final String node1;
        public final String node2;

        public CreateResult(String node1, String node2) {
            this.node1 = node1;
            this.node2 = node2;
        }
    }

}

