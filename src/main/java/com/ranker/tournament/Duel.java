package com.ranker.tournament;

import java.io.Serializable;

/**
 * This class operates with similar intentions as an immutable Tuple<T> in that it has 
 * symmetric parameters such that:
 * 
 * new Duel(candidate1, candidate2).equals(new Duel(candidate2, candidate1)) will yield `true`
 * 
 * This class pairs with DuelWinner to paint the picture of a Tuple, but where a one of the 2 elements
 * `a` or `b` will win the duel.
 * 
 * @author Marcus
 * @date 8/15/2021
 * @param <T> the typing of the 2 candidates ready to compete
 */
public class Duel<T> implements Serializable {
	
	private static final long serialVersionUID = 1181755518208435223L;
	
	/** candidate A in the head-to-head duel */
	private final T a;
	
	/** candidate B in the head-to-head duel */
	private final T b;
	
	/**
	 * Defines the candidates in the duel, allowing for any type T
	 * 
	 * @param a first candidate in the Duel
	 * @param b second candidate in the Duel
	 */
	public Duel(T a, T b) {
		this.a = a;
		this.b = b;
	}
	
	
	/**
	 * Standard getter for A
	 * 
	 * @return current value of A
	 */
	public T getA() {
		return a;
	}
	
	
	/**
	 * Standard getter for B
	 * 
	 * @return current value of B
	 */
	public T getB() {
		return b;
	}
	
	
	/**
	 * Standard override of `Object.equals()` to define a symmetric property that if
	 * 2 Duels have the same elements but in different locations, then the Duels are
	 * considered equal.
	 */
	public boolean equals(Object o) {
		if(o == this)
			return true;
		if(!(o instanceof Duel))
			return false;
		
		@SuppressWarnings("unchecked")
		Duel<T> d = (Duel<T>) o;
		return (a.equals(d.a) && b.equals(d.b)) || (a.equals(d.b)&& b.equals(d.a));
	}
	
}
