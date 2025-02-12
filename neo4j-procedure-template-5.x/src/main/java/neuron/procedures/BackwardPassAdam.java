package neuron.procedures;

import org.eclipse.jetty.util.Index;
import org.neo4j.graphdb.*;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.stream.Stream;
import java.util.Map;

public class BackwardPassAdam {

    /* @Context to access Log Service
       and GraphDatabaseService from Neo4j
    */
    @Context
    public Log log;

    @Context
    public GraphDatabaseService db;

    // Creating Procedure <nn.backwardPassAdamStep1>

    @Procedure(name = "nn.backwardPassAdamStep1", mode = Mode.WRITE)
    @Description("Executing backward pass Adam Step 1 to update Output layer for NeuralNetwork")

    public Stream<CreateResult> adamStepOne(@Name("learning_rate") Double learning_rate,
                                            @Name("beta1") Double beta1,
                                            @Name("beta2") Double beta2,
                                            @Name("epsilon") Double epsilon,
                                            @Name("iteration") Long t) {

        try (Transaction tx = db.beginTx()) {

            String query = """
                    MATCH (n:Neuron {type: 'hidden'})<-[:CONNECTED_TO]-(next:Neuron)
                    WITH n, next, $t AS t
                    MATCH (n)-[r:CONNECTED_TO]->(next)
                    WITH n, SUM(next.gradient * COALESCE(r.weight, 0)) AS raw_gradient, t
                    WITH n,
                         CASE 
                             WHEN n.activation_function = 'relu' THEN CASE WHEN n.output > 0 THEN raw_gradient ELSE 0 END
                             WHEN n.activation_function = 'sigmoid' THEN raw_gradient * n.output * (1 - n.output)
                             WHEN n.activation_function = 'tanh' THEN raw_gradient * (1 - n.output^2)
                             ELSE raw_gradient  // For linear activation
                         END AS gradient, t
                    MATCH (prev:Neuron)-[r_prev:CONNECTED_TO]->(n)
                    SET r_prev.m = $beta1 * COALESCE(r_prev.m, 0) + (1 - $beta1) * gradient * COALESCE(prev.output, 0)
                    SET r_prev.v = $beta2 * COALESCE(r_prev.v, 0) + (1 - $beta2) * (gradient * COALESCE(prev.output, 0))^2
                    SET r_prev.weight = r_prev.weight - $learning_rate * (r_prev.m / (1 - ($beta1 ^ t))) / 
                                        (SQRT(r_prev.v / (1 - ($beta2 ^ t))) + $epsilon)
                    SET n.m_bias = $beta1 * COALESCE(n.m_bias, 0) + (1 - $beta1) * gradient
                    SET n.v_bias = $beta2 * COALESCE(n.v_bias, 0) + (1 - $beta2) * (gradient^2)
                    SET n.bias = n.bias - $learning_rate * (n.m_bias / (1 - ($beta1 ^ t))) / 
                                 (SQRT(n.v_bias / (1 - ($beta2 ^ t))) + $epsilon)
                    SET n.gradient = gradient
                    RETURN *
                    """;
            Result queryresult = tx.execute(query, Map.of("learning_rate", learning_rate,
                                     "beta1", beta1,
                                     "beta2", beta2,
                                     "epsilon", epsilon,
                                     "t",t)
                      );
            Stream<CreateResult> Stream_output = queryresult.stream()
                                                            .map(row-> new CreateResult(row.toString()));
            tx.commit();

            log.info("Backward Pass Adam Step One completed successfully !");

            return Stream_output;

        } catch (Exception e) {

            log.error("Error executing:" + e.getMessage());

            return Stream.of(new CreateResult("Failure :(" + e.getMessage()));
        }
    }

    // Creating Procedure <nn.backwardPassAdamStep2>
    @Procedure(name = "nn.backwardPassAdamStep2", mode = Mode.WRITE)
    @Description("Executing backward pass Adam Step 2 to update Output layer for NeuralNetwork")

    public Stream<CreateResult> adamStepTwo(@Name("learning_rate") Double learning_rate,
                                            @Name("beta1") Double beta1,
                                            @Name("beta2") Double beta2,
                                            @Name("epsilon") Double epsilon,
                                            @Name("iteration") Long t) {

        try (Transaction tx = db.beginTx()) {

            String query = """
                    MATCH (n:Neuron {type: 'hidden'})<-[:CONNECTED_TO]-(next:Neuron)
                    WITH n, next, $t AS t
                    MATCH (n)-[r:CONNECTED_TO]->(next)
                    WITH n, SUM(next.gradient * COALESCE(r.weight, 0)) AS raw_gradient, t
                    WITH n,
                         CASE 
                             WHEN n.activation_function = 'relu' THEN CASE WHEN n.output > 0 THEN raw_gradient ELSE 0 END
                             WHEN n.activation_function = 'sigmoid' THEN raw_gradient * n.output * (1 - n.output)
                             WHEN n.activation_function = 'tanh' THEN raw_gradient * (1 - n.output^2)
                             ELSE raw_gradient  // For linear activation
                         END AS gradient, t
                    MATCH (prev:Neuron)-[r_prev:CONNECTED_TO]->(n)
                    SET r_prev.m = $beta1 * COALESCE(r_prev.m, 0) + (1 - $beta1) * gradient * COALESCE(prev.output, 0)
                    SET r_prev.v = $beta2 * COALESCE(r_prev.v, 0) + (1 - $beta2) * (gradient * COALESCE(prev.output, 0))^2
                    SET r_prev.weight = r_prev.weight - $learning_rate * (r_prev.m / (1 - ($beta1 ^ t))) / 
                                        (SQRT(r_prev.v / (1 - ($beta2 ^ t))) + $epsilon)
                    SET n.m_bias = $beta1 * COALESCE(n.m_bias, 0) + (1 - $beta1) * gradient
                    SET n.v_bias = $beta2 * COALESCE(n.v_bias, 0) + (1 - $beta2) * (gradient^2)
                    SET n.bias = n.bias - $learning_rate * (n.m_bias / (1 - ($beta1 ^ t))) / 
                                 (SQRT(n.v_bias / (1 - ($beta2 ^ t))) + $epsilon)
                    SET n.gradient = gradient
                    RETURN *
                    """;
            Result query_result = tx.execute(query, Map.of("learning_rate", learning_rate,
                                     "beta1", beta1,
                                     "beta2", beta2,
                                     "epsilon", epsilon,
                                     "t",t)
                        );
            Stream<CreateResult> Stream_output = query_result.stream()
                                                             .map(element->new CreateResult(element.toString()));

            tx.commit();

            log.info("Backward Pass Adam Step Two completed successfully !");

            return Stream_output;

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

