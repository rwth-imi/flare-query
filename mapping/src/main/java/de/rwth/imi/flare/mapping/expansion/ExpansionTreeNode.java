package de.rwth.imi.flare.mapping.expansion;

import de.rwth.imi.flare.api.model.TerminologyCode;
import lombok.Data;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Data
public class ExpansionTreeNode {
    private List<ExpansionTreeNode> children;
    private TerminologyCode termCode;

    /**
     * Searches for the node containing a given code
     * @param searchedCode code the returned node should contain
     * @return TerminologyCode containing the searchedCode if found, null if no node was found
     */
    public ExpansionTreeNode findTermCode(TerminologyCode searchedCode){
        Predicate<ExpansionTreeNode> findTermCodePredicate = node -> node.termCode.equals(searchedCode);
        return this.bfs(findTermCodePredicate);
    }

    /**
     * Executes a breadth first search on the Tree executing a given predicate on each node determining when to abort
     *
     * @param abortSearchPredicate Predicate that returns true when the bfs should be aborted
     * @return The node on which the predicate decided to terminate, or null if it doesn't terminate
     */
    public ExpansionTreeNode bfs(Predicate<ExpansionTreeNode> abortSearchPredicate){
        Queue<ExpansionTreeNode> encounteredNodes = new LinkedList<>(List.of(new ExpansionTreeNode[]{this}));


        while(!encounteredNodes.isEmpty()){
            ExpansionTreeNode currentNode = encounteredNodes.poll();

            if(currentNode.children != null) {
                encounteredNodes.addAll(currentNode.children);
            }

            if(abortSearchPredicate.test(currentNode)){
                return currentNode;
            }
        }
        return null;
    }

    /**
     * Traverses the tree in a breadth first manner and executes a consumer on each node encountered
     * @param nodeConsumer consumer of nodes
     */
    public void bfs(Consumer<ExpansionTreeNode> nodeConsumer){
        // Derive predicate from consumer
        Predicate<ExpansionTreeNode> derivedPredicate = expansionTreeNode -> {
            nodeConsumer.accept(expansionTreeNode);
            return false;
        };

        // Execute bfs with derived predicate
        this.bfs(derivedPredicate);
    }
}
