package neuron.procedures;

import org.eclipse.jetty.util.Index;
import org.neo4j.graphdb.*;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.util.stream.Stream;
import java.util.Map;

public class ForwardPass {

    /* @Context to access Log Service
       and GraphDatabaseService from Neo4j
    */
    @Context
    public Log log;

    @Context
    public GraphDatabaseService db;

    // Creating Procedure <nn.forwardPass>

    @Procedure(name = "nn.forwardPass", mode = Mode.WRITE)
    @Description("Forward Pass for NeuralNetwork")

    public Stream<CreateResult> forwardPass() {

        try (Transaction tx = db.beginTx()) {

            String query = """
        
            MATCH (row_for_inputs:Row {type: 'inputsRow'})-[inputsValue_R:CONTAINS]->(input:Neuron {type: 'input'})
            MATCH (input)-[r1:CONNECTED_TO]->(hidden:Neuron {type: 'hidden'})
            MATCH (hidden)-[r2:CONNECTED_TO]->(output:Neuron {type: 'output'})
            MATCH (output)-[outputsValues_R:CONTAINS]->(row_for_outputs:Row {type: 'outputsRow'})
            WITH DISTINCT row_for_inputs,inputsValue_R, input,r1,hidden,r2,output ,outputsValues_R,row_for_outputs,
        
            SUM(COALESCE(outputsValues_R.output, 0) * r1.weight) AS weighted_sum
            SKIP 0 LIMIT 1000
            SET hidden.output = CASE 
                WHEN hidden.activation_function = 'relu' THEN CASE WHEN (weighted_sum + hidden.bias) > 0 THEN (weighted_sum + hidden.bias) ELSE 0 END
                WHEN hidden.activation_function = 'sigmoid' THEN 1 / (1 + EXP(-(weighted_sum + hidden.bias)))
                WHEN hidden.activation_function = 'tanh' THEN (EXP(2 * (weighted_sum + hidden.bias)) - 1) / (EXP(2 * (weighted_sum + hidden.bias)) + 1)
                ELSE weighted_sum + hidden.bias
            END
			
	        WITH row_for_inputs,inputsValue_R, input,r1,hidden,r2,output ,outputsValues_R,row_for_outputs,
	        SUM(COALESCE(hidden.output, 0) * r2.weight) AS weighted_sum
            SET outputsValues_R.output = CASE 
                WHEN output.activation_function = 'softmax' THEN weighted_sum  //Temporary value; softmax applied later
                WHEN output.activation_function = 'sigmoid' THEN 1 / (1 + EXP(-(weighted_sum + output.bias)))
                WHEN output.activation_function = 'tanh' THEN (EXP(2 * (weighted_sum + output.bias)) - 1) / (EXP(2 * (weighted_sum + output.bias)) + 1)
                ELSE weighted_sum + output.bias
            END
	        WITH COLLECT(output) AS output_neurons, COLLECT(outputsValues_R) AS outputsValues_Rs
               WITH output_neurons, outputsValues_Rs,
                    [n IN outputsValues_Rs | exp(COALESCE(n.output, 0))] AS exp_outputs,
                    [n IN output_neurons | n.activation_function] AS activation_functions
               WITH output_neurons, outputsValues_Rs, exp_outputs, activation_functions, 
                    REDUCE(sum = 0.0, x IN exp_outputs | sum + x) AS sum_exp_outputs
               UNWIND RANGE(0, SIZE(output_neurons) - 1) AS i
               UNWIND RANGE(0, SIZE(outputsValues_Rs) - 1) AS j
               WITH output_neurons[i] AS neuron,outputsValues_Rs[j] AS outputRow, exp_outputs[i] AS exp_output, 
                    activation_functions[i] AS activation_function, sum_exp_outputs
               WITH neuron,outputRow, 
                    CASE 
                        WHEN activation_function = 'softmax' THEN exp_output / sum_exp_outputs
                        ELSE outputRow.output
                    END AS adjusted_output
               SET outputRow.output = adjusted_output
               RETURN input, hidden, output
        """;

            Result query_result = tx.execute(query);
            Stream<CreateResult> streamout = query_result.stream()
                                                         .map(x->new CreateResult(x.get("input").toString(),
                                                                                                    x.get("hidden").toString(),
                                                                                                    x.get("output").toString()
                                                                                                    )
                                                             );

            tx.commit();

            log.info(String.format("Query successfuly executed!"));

            return streamout;

        } catch (Exception e) {

            log.error("Error executing:" + e.getMessage());

            return Stream.of(new CreateResult("Failure :(" + e.getMessage(), null, null));
        }
    }

    public static class CreateResult {

        public final String result1;
        public final String result2;
        public final String result3;


        public CreateResult(String result1,String result2,String result3) {
            this.result1 = result1;
            this.result2 = result2;
            this.result3 = result3;
        }
    }
}
