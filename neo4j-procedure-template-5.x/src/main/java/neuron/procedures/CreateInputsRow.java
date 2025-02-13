package neuron.procedures;

import org.neo4j.graphdb.*;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.stream.Stream;
import java.util.Map;

public class CreateInputsRow {

    /* @Context to access Log Service
       and GraphDatabaseService from Neo4j
    */
    @Context
    public Log log;
    @Context
    public GraphDatabaseService db;

    // Creating Procedure <nn.createInputsRow>

    @Procedure(name = "nn.createInputsRow", mode = Mode.WRITE)
    @Description("Create Inputs Rows for NeuralNetwork")

    public Stream<CreateResult> createInputsRow(@Name("id") String id) {

        try (Transaction tx = db.beginTx()) {

            String creating_query = """
                                        CREATE (n:Row {id: $id,
                                                        type: 'inputsRow'})
                                        Return n
                                    """;

            Result query_result = tx.execute(creating_query, Map.of("id",id)
                                            );
            Stream<CreateResult> streamout = query_result.stream()
                                                         .map(element->new CreateResult(element.toString(),
                                                                                                            null));
            tx.commit();

            log.info(String.format("New Row with id: %s was created ! ",
                                    id));

            return streamout;

        } catch (Exception e) {

            log.error("Error creating Row:" + e.getMessage());

            return Stream.of(new CreateResult("Failure :(" + e.getMessage(),null));
        }
    }

    // Creating Procedure <nn.connexionInputsNeurons>

    @Procedure(name = "nn.connexionInputsNeurons", mode = Mode.WRITE)
    @Description("Create connection between Inputs and Neurons")

    public Stream<CreateResult> ConnexionStream(@Name("from_id") String from_id,
                                                @Name("to_id") String to_id,
                                                @Name("value") Double value,
                                                @Name("inputfeatureid") String inputfeatureid) {

        if (value == null) {value = 0.0; // Assigning default value 0.0
                            }

        try (Transaction tx = db.beginTx()) {

            String connexion_query = """
                                        MATCH (n1:Row {id: $from_id,type:'inputsRow'})
                                        MATCH (n2:Neuron {id: $to_id,type:'input'})
                                        CREATE (n1)-[:CONTAINS {output: $value,id:$inputfeatureid}]->(n2)
                                        RETURN n1, n2
                                    """;

            Result query_result = tx.execute(connexion_query, Map.of("from_id", from_id,
                                               "to_id", to_id,
                                               "value", value,
                                               "inputfeatureid", inputfeatureid)
                                            );
            Stream<CreateResult> streamout = query_result.stream()
                                                         .map(x->new CreateResult(x.get("n1").toString(),
                                                                                                    x.get("n2").toString()
                                                                                                    )
                                                             );

            tx.commit();

            log.info(String.format("New Connexion successfuly established " +
                        "from inputRow: %s to Neuron: %s",from_id,to_id));

            return streamout;

        } catch (Exception e) {

            log.error("Error creating Connexion:" + e.getMessage());

            return Stream.of(new CreateResult("Failure :)", null));
        }
    }

    public static class CreateResult {

        public final String result1;
        public final String result2;

        public CreateResult(String result1, String result2) {
            this.result1 = result1;
            this.result2 = result2;
        }
    }

}

