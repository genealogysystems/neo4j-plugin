package com.findarecord.neo4j.plugin;


import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.UniqueFactory;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EntryDelete {

  private GraphDatabaseService graphDb;

  private Node entryNode;

  private HashSet<String> incrementedNodes;

  public EntryDelete(GraphDatabaseService graphDb) {
    this.graphDb = graphDb;
    this.incrementedNodes = new HashSet<>();
  }

  public void deleteEntry(String entryId) {
    try ( Transaction tx = graphDb.beginTx() ) {
      UniqueFactory<Node> factory = new UniqueFactory.UniqueNodeFactory( graphDb, Settings.NEO_ENTRY)
      {
        @Override
        protected void initialize( Node created, Map<String, Object> properties )
        {
          created.setProperty( "id", properties.get( "id" ) );
        }
      };

      //get entry node
      entryNode = factory.getOrCreate("id", entryId);

      //decrement counters
      TraversalDescription traversal = graphDb.traversalDescription()
          .breadthFirst()
          .relationships(DynamicRelationshipType.withName(Settings.NEO_BOX_LINK), Direction.OUTGOING)
          .relationships(DynamicRelationshipType.withName(Settings.NEO_BOX_INTERSECT), Direction.INCOMING)
          .evaluator(getEvaluator());

      for(Path path : traversal.traverse(entryNode)) {
        Integer count = (Integer) path.endNode().getProperty("count");
        path.endNode().setProperty("count", count - 1);
      }

      //remove all old relationships
      for(Relationship rel: entryNode.getRelationships()) {
        rel.delete();
      }
      entryNode.delete();
      tx.success();
    }
  }

  private Evaluator getEvaluator() {
    return new Evaluator() {
      @Override
      public Evaluation evaluate( final Path path )
      {
        if ( path.length() == 0 ) {
          return Evaluation.EXCLUDE_AND_CONTINUE;
        } else {
        return Evaluation.INCLUDE_AND_CONTINUE;
        }
      }
    };
  }
}
