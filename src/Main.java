import be.ugent.rml.Executor;
import be.ugent.rml.Utils;
import be.ugent.rml.functions.FunctionLoader;
import be.ugent.rml.functions.lib.IDLabFunctions;
import be.ugent.rml.records.RecordsFactory;
import be.ugent.rml.store.QuadStore;
import be.ugent.rml.store.QuadStoreFactory;
import be.ugent.rml.store.RDF4JStore;
import be.ugent.rml.term.NamedNode;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.eclipse.rdf4j.rio.RDFFormat;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class Main {

    public static void Main(){
        RMLPipeline pipeline = new RMLPipeline();

        String rootFolder = "./";
        String templateFile = "airports/steamreviews_tsintzask_mapping.ttl";
        String outputFile = "airports/steamreviews_tsintzask_mapper_output.ttl";

        String mapPath = rootFolder + templateFile;
        File mappingFile = new File(mapPath);

        String outPath = rootFolder + outputFile;
        Writer output = new FileWriter(outPath);

        pipeline.runRMLMapper(mappingFile, output);

        HTTPRepository repository = new HTTPRepository("http://localhost:7200/repositories/steamreviews_kgoe_tsintzask");
        RepositoryConnection connection = repository.getConnection();

        connection.clear();
        connection.begin();
        try {
            connection.add(RMLPipeline.class.getResourceAsStream("/rml/airports/steamreviews_tsintzask.ttl"),
                    "urn:base",
                    RDFFormat.TURTLE);
            connection.add(GraphDBExample.class.getResourceAsStream("/rml/airports/outputFile.ttl"),
                    "urn:base",
                    RDFFormat.TURTLE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        connection.commit();

        // SPARQL queries

        // Counts the number of video games in the dataset.
        String queryString = "prefix vgo: <http://purl.org/net/VideoGameOntology#> \n";
        queryString += "select (COUNT(?game) AS ?count) { \n";
        queryString += "?game a vgo:Game \n";
        queryString += "}";

        TupleQuery query = connection.prepareTupleQuery(queryString);

        try (TupleQueryResult result = query.evaluate()) {
            for (BindingSet solution : result) {
                System.out.println("?count = " + solution.getValue("count"));
            }
        }

        // Counts the number of users in the dataset.
        queryString = "prefix grts:<http://www.semanticweb.org/tsintzask/ontologies/2024/6/steamreviews-tsintzask#> \n";
        queryString += "select (COUNT(?user) AS ?count) { \n";
        queryString += "?user a grts:SteamUser \n";
        queryString += "}";

        query = connection.prepareTupleQuery(queryString);

        try (TupleQueryResult result = query.evaluate()) {
            for (BindingSet solution : result) {
                System.out.println("?count = " + solution.getValue("count"));
            }
        }

        // Counts the number of reviews in the dataset.
        queryString = "prefix grts:<http://www.semanticweb.org/tsintzask/ontologies/2024/6/steamreviews-tsintzask#> \n";
        queryString += "select (COUNT(?review) AS ?count) { \n";
        queryString += "?user a grts:GameReview \n";
        queryString += "}";

        query = connection.prepareTupleQuery(queryString);

        try (TupleQueryResult result = query.evaluate()) {
            for (BindingSet solution : result) {
                System.out.println("?count = " + solution.getValue("count"));
            }
        }

        // Counts positive reviews for each video game and displays the top 10.
        queryString = "prefix grts:<http://www.semanticweb.org/tsintzask/ontologies/2024/6/steamreviews-tsintzask#> \n";
        queryString += "select ?game (COUNT(?review) AS ?count) { \n";
        queryString += "?game grts:HasReview ?review. \n";
        queryString += "?review grts:PositiveRating True. \n";
        queryString += "} group by ?game order by desc(?count) limit 10";

        query = connection.prepareTupleQuery(queryString);

        try (TupleQueryResult result = query.evaluate()) {
            for (BindingSet solution : result) {
                System.out.println("?game =" + solution.getValue("game") + ", ?count = " + solution.getValue("count"));
            }
        }

        // Counts negative reviews for each video game and displays the worst 10.
        queryString = "prefix grts:<http://www.semanticweb.org/tsintzask/ontologies/2024/6/steamreviews-tsintzask#> \n";
        queryString += "select ?game (COUNT(?review) AS ?count) { \n";
        queryString += "?game grts:HasReview ?review. \n";
        queryString += "?review grts:PositiveRating False. \n";
        queryString += "} group by ?game order by desc(?count) limit 10";

        query = connection.prepareTupleQuery(queryString);

        try (TupleQueryResult result = query.evaluate()) {
            for (BindingSet solution : result) {
                System.out.println("?game =" + solution.getValue("game") + ", ?count = " + solution.getValue("count"));
            }
        }

        // Counts reviews written by each user and displays the top 10.
        // Note: User tags are the first 5 characters of their Steam ID's SHA256 hash, so it's possible some or most of these are collisions.
        queryString = "prefix grts:<http://www.semanticweb.org/tsintzask/ontologies/2024/6/steamreviews-tsintzask#> \n";
        queryString += "select ?user (COUNT(?review) AS ?count) { \n";
        queryString += "?user grts:Wrote ?review. \n";
        queryString += "} group by ?game order by desc(?count) limit 10";

        query = connection.prepareTupleQuery(queryString);

        try (TupleQueryResult result = query.evaluate()) {
            for (BindingSet solution : result) {
                System.out.println("?user =" + solution.getValue("user") + ", ?count = " + solution.getValue("count"));
            }
        }

        connection.close();
        repository.shutDown();

    }
}