package neuron.procedures;

import org.eclipse.jetty.util.Index;
import org.neo4j.graphdb.*;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.stream.Stream;
import java.util.Map;

public class ComputeLoss {

    /* @Context to access Log Service
       and GraphDatabaseService from Neo4j
    */
    @Context
    public Log log;

    @Context
    public GraphDatabaseService db;

    // Creating Procedure <nn.classification>

    @Procedure(name = "nn.classification", mode = Mode.WRITE)
    @Description("Run A NeuralNetwork for Classification Tasks")

    public Stream<CreateResult> classification() {

        try (Transaction tx = db.beginTx()) {

            String query = """
                                MATCH (output:Neuron {type: 'output'})
                                MATCH (output)-[outputsValues_R:CONTAINS]->(row_for_outputs:Row {type: 'outputsRow'})
                                WITH outputsValues_R,
                                     COALESCE(outputsValues_R.output, 0) AS predicted,
                                     COALESCE(outputsValues_R.expected_output, 0) AS actual,
                                     1e-10 AS epsilon
                                RETURN SUM(
                                    -actual * LOG(predicted + epsilon) - (1 - actual) * LOG(1 - predicted + epsilon)
                                ) AS loss
                           """;
            tx.execute(query);

            tx.commit();

            log.info("Classification computed successfully !");

            return Stream.of(new CreateResult("Success :)"));

        } catch (Exception e) {

            log.error("Error executing:" + e.getMessage());

            return Stream.of(new CreateResult("Failure :(" + e.getMessage()));
        }
    }

    // Creating Procedure <nn.regression>
    @Procedure(name = "nn.regression", mode = Mode.WRITE)
    @Description("Run A NeuralNetwork for Regression Tasks")

    public Stream<CreateResult> regression() {

        try (Transaction tx = db.beginTx()) {

            String query = """
                                 MATCH (output:Neuron {type: 'output'})
                                 MATCH (output)-[outputsValues_R:CONTAINS]->(row_for_outputs:Row {type: 'outputsRow'})
                                 WITH outputsValues_R,
                                     COALESCE(outputsValues_R.output, 0) AS predicted,
                                     COALESCE(outputsValues_R.expected_output, 0) AS actual
                                RETURN AVG((predicted - actual)^2) AS loss
                    """;
            tx.execute(query);

            tx.commit();

            log.info("Regression computed successfully !");

            return Stream.of(new CreateResult("Success :)"));

        } catch (Exception e) {

            log.error("Error executing:" + e.getMessage());

            return Stream.of(new CreateResult("Failure :(" + e.getMessage()));
        }
    }

    public static class CreateResult {

        public final String result;

        public CreateResult(String result) {
            this.result = result;
        }
    }

}


