package org.eclipse.draw2d.internal.graph;

import org.eclipse.draw2d.graph.DirectedGraph;
import org.eclipse.draw2d.graph.Rank;

/**
 * @author hudsonr
 * @since 2.1
 */
public class MinCross extends GraphVisitor {

static final int MAX = 45;

private DirectedGraph g;
private RankSorter sorter = new RankSorter();

public void setRankSorter(RankSorter sorter) {
	this.sorter = sorter;
}

void solve() {
	Rank rank;
	for (int loop = 0; loop < MAX; loop++) {
		for (int row = 1; row < g.ranks.size(); row++) {			
			rank = g.ranks.getRank(row);
			sorter.sortRankIncoming(g, rank, row, (double)loop / MAX);
		}
		if (loop == MAX - 1)
			continue;
		for (int row = g.ranks.size() - 2; row >= 0; row--) {
			rank = g.ranks.getRank(row);
			sorter.sortRankOutgoing(g, rank, row, (double)loop / MAX);
		}
	}
}

/**
 *  @see GraphVisitor#visit(org.eclipse.draw2d.graph.DirectedGraph)
 */
public void visit(DirectedGraph g) {
	sorter.init(g);
	this.g = g;
	solve();
}

}
