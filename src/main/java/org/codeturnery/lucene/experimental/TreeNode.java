package org.codeturnery.lucene.experimental;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.codeturnery.tree.TreeNodeInterface;

public class TreeNode<K, V> implements TreeNodeInterface<TreeNode<K, V>> {

	private final TreeNode<K, V> parent;
	private final List<V> children;
	private Map<K, TreeNode<K, V>> subNodes;

	public TreeNode(final TreeNode<K, V> parent) {
		this.parent = parent;
		this.children = new ArrayList<>();
		this.subNodes = new LinkedHashMap<>();
	}
	
	@Override
	public TreeNode<K, V> getParent() {
		return this.parent;
	}

	public Map<K, TreeNode<K, V>> getSubNodes() {
		return Collections.unmodifiableMap(this.subNodes);
	}

	public Collection<V> getChildren() {
		return Collections.unmodifiableCollection(this.children);
	}

	/**
	 * Adds the given child to this node or a sub node depending on the given keys.
	 * <p>
	 * Each element in the given iterator corresponds to a different layer in the
	 * tree. The keys in each layer define to what sub nodes the child in that layer
	 * should be added.
	 * <p>
	 * Example: add the child directly to this node.
	 * 
	 * <pre>
	 * [ ] // an empty iterator
	 * </pre>
	 * <p>
	 * Example: add the child to the sub nodes <code>A</code> and <code>B</code>
	 * inside this node.
	 * 
	 * <pre>
	 * [ // iterator with one element
	 *   [ A, B ] // iterable with two keys
	 * ]
	 * </pre>
	 * <p>
	 * Example: add the child to two sub nodes, both named <code>x</code>. One being
	 * a sub node of <code>A</code>, which is located inside this node and the other
	 * one being a sub node of <code>B</code>, which is is also located inside this
	 * node.
	 * 
	 * <pre>
	 * [ // iterator with two elements
	 *   [ A, B ], // iterable with the two keys denoting sub nodes inside this node
	 *   [ x ] // iterable with a single key denoting two different nodes inside A and B
	 * ]
	 * </pre>
	 * 
	 * <p>
	 * Example: add the child to three sub nodes <code>x</code>, <code>y</code> and
	 * <code>z</code>, all located inside <code>A</code>, which is a sub node of
	 * this node.
	 * 
	 * <pre>
	 * [ // iterator with two elements
	 *   [ A ], // iterable with one key denoting a sub node inside this node
	 *   [ x, y, z ] // iterable denoting three sub nodes, each one being a sub node of A
	 * ]
	 * </pre>
	 * <p>
	 * If one of the iterables inside the given iterator is empty the child will not
	 * be added to any node.
	 * 
	 * <pre>
	 * [ // iterator with two elements
	 *   [ A ],
	 *   [],
	 *   [ x, y ]
	 * ]
	 * </pre>
	 * <p>
	 * Add the child six times to a sub node <code>A</code> located inside a (different) sub
	 * node <code>A</code>, which is located inside this node.
	 * 
	 * <pre>
	 * [ // iterator with two elements
	 *   [ A, A ],
	 *   [ A, A, A ]
	 * ]
	 * </pre>
	 * 
	 * @param child The child to add.
	 * @param keys  The keys to follow to select the correct node the child is added
	 *              to.
	 */
	public void addChild(final V child, final Iterator<Iterable<K>> keys) {
		if (keys.hasNext()) {
			for (final K subNodeKey : keys.next()) {
				this.getSubNode(subNodeKey).addChild(child, keys);
			}
		} else {
			this.children.add(child);
		}
	}
	
	public void addChild(final V child) {
		this.children.add(child);
	}
	
	public void addSubNode(final TreeNode<K,V> node, final K key) {
		this.subNodes.put(key, node);
	}

	/**
	 * Get a sub node by its key from the already created sub nodes. If no sub node
	 * for the key exists yet it will be created and added to the given sub nodes.
	 * 
	 * @param key The key the bucket should be get for. Must not be
	 *            <code>null</code>.
	 * @return The sub node corresponding to the given key. Must not be
	 *         <code>null</code>.
	 */
	protected TreeNode<K, V> getSubNode(final K key) {
		return this.subNodes.computeIfAbsent(key, k -> new TreeNode<>(this));
	}
}
