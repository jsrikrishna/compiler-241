package main.edu.uci.compiler.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by raghugudipati on 2/28/17.
 */
public class Multimap<T> extends HashMap<T, Set<T>> {
    /**
     * Fetch the set for a given key, creating it if necessary.
     *
     * @param key - the key.
     * @return the set of values mapped to the key.
     */
    @SuppressWarnings("unchecked")
    @Override
    public Set<T> get(Object key) {
        if (!this.containsKey(key))
            this.put((T) key, new HashSet<T>());

        return super.get(key);
    }
}
