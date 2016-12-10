package ruleGenerators.geneticRuleGenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import core.ArcadeMachine;
import core.game.SLDescription;
import core.game.StateObservation;
import core.generator.AbstractRuleGenerator;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.LevelMapping;

public class RuleGenerator extends AbstractRuleGenerator {
	/**
	 * Defines how large the population will be
	 */
	private final int POP_SIZE = 100;
	/**
	 * ArrayList that contains the population
	 */
	private ArrayList<Chromosome> population;

	/**
	 * A list of all the useful sprites in the game
	 */
	private ArrayList<String> usefulSprites;
	/**
	 * Random object to help in generation
	 */
	private Random random;
	/**
	 * A constructor to help speed up the creation of random gens
	 */
	private static ruleGenerators.randomRuleGenerator.RuleGenerator randomGen;

	/**
	 * A constructor to help speed up the creation of constructive gens
	 */
	private static ruleGenerators.constructiveRuleGenerator.RuleGenerator constructGen;
	/**
	 * contains info about sprites and current level
	 */
	private SLDescription sl;
	/**
	 * amount of time allowed
	 */
	private ElapsedCpuTimer time;
	/**
	 * chance of mutation
	 */
	private double MUTATION_CHANCE = 0.1;
	/**
	 * elitism coefficient
	 */
	private double ELITISM = 0.1;
	/**
	 * chance of crossover
	 */
	private double CROSSOVER_CHANCE = 0.1;
	/**
	 * Level mapping of the best chromosome
	 */
	private String[][] bestChromosomeRuleset;
	/**
	 * The best chromosome fitness across generations
	 */
	private ArrayList<Double> bestFitness;
	/**
	 * number of feasible chromosomes across generations
	 */
	private ArrayList<Integer> numOfFeasible;
	/**
	 * number of infeasible chromosomes across generations
	 */
	private ArrayList<Integer> numOfInFeasible;
	
	
	/**
	 * This is an evolutionary rule generator
	 * @param sl	contains information about sprites and current level
	 * @param time	amount of time allowed
	 */
	public RuleGenerator(SLDescription sl, ElapsedCpuTimer time) {
		this.sl = sl;
		this.time = time;
		this.usefulSprites = new ArrayList<String>();
		SharedData.random = new Random();

		String[][] currentLevel = sl.getCurrentLevel();
		//Just get the useful sprites from the current level
		for (int y = 0; y < currentLevel.length; y++) {
			for (int x = 0; x < currentLevel[y].length; x++) {
				String[] parts = currentLevel[y][x].split(",");
				for (int i = 0; i < parts.length; i++) {
					if (parts[i].trim().length() > 0) {
						//Add the sprite if it doesn't exisit
						if (!usefulSprites.contains(parts[i].trim())) {
							usefulSprites.add(parts[i].trim());
						}
					}
				}
			}
		}
		// iterate through population and mutate
	}

	/**
	 * convert the arraylist of string to a normal array of string
	 * @param list	input arraylist
	 * @return		string array
	 */
	 private String[] getArray(ArrayList<String> list){
		 String[] array = new String[list.size()];
		 for(int i=0; i<list.size(); i++){
			 array[i] = list.get(i);
		 }
		 return array;
	 }
	/**
	 * Initializes the population with 100 RandomGenerators
	 */
	public void initPop(SLDescription sl, ElapsedCpuTimer time) {
		population = new ArrayList<Chromosome>();

		
		
	}
	
	/**
	 * The first fitness function. This will eliminate games that are not feasible to play
	 */
	public void feasibilityTest() {
		// test ruleset for robustness
		for(Chromosome chrome : population) {
			if(!chrome.feasibilityTest()){
				population.remove(chrome);
			}
		}
	}
	
	/**
	 * Get the next population based on the current feasible infeasible population
	 * @param fPopulation	array of the current feasible chromosomes
	 * @param iPopulation	array of the current infeasible chromosomes
	 * @return				array of the new chromosomes at the new population
	 */
	private ArrayList<Chromosome> getNextPopulation(ArrayList<Chromosome> fPopulation, ArrayList<Chromosome> iPopulation){
		ArrayList<Chromosome> newPopulation = new ArrayList<Chromosome>();
		
		//collect some statistics about the current generation
		ArrayList<Double> fitnessArray = new ArrayList<Double>();
		for(int i=0;i<fPopulation.size();i++){
			fitnessArray.add(fPopulation.get(i).getFitness().get(0));
		}

		Collections.sort(fitnessArray);
		if(fitnessArray.size() > 0){
			bestFitness.add(fitnessArray.get(fitnessArray.size() - 1));
		}
		else{
			bestFitness.add((double) 0);
		}
		numOfFeasible.add(fPopulation.size());
		numOfInFeasible.add(iPopulation.size());
		

		
		while(newPopulation.size() < SharedData.POPULATION_SIZE){
			//choosing which population to work on with 50/50 probability 
			//of selecting either any of them
			ArrayList<Chromosome> population = fPopulation;
			if(fPopulation.size() <= 0){
				population = iPopulation;
			}
			if(SharedData.random.nextDouble() < 0.5){
				population = iPopulation;
				if(iPopulation.size() <= 0){
					population = fPopulation;
				}
			}
			

			//select the parents using roulettewheel selection
			Chromosome parent1 = rouletteWheelSelection(population);
			Chromosome parent2 = rouletteWheelSelection(population);
			Chromosome child1 = parent1.clone();
			Chromosome child2 = parent2.clone();
			//do cross over
			if(SharedData.random.nextDouble() < SharedData.CROSSOVER_PROB){
				ArrayList<Chromosome> children = parent1.crossOverIteraction(parent2);
				child1 = children.get(0);
				child2 = children.get(1);
				

				//do muation to the children
				if(SharedData.random.nextDouble() < SharedData.MUTATION_PROB){
					child1.mutate();
				}
				if(SharedData.random.nextDouble() < SharedData.MUTATION_PROB){
					child2.mutate();
				}
			}

			//mutate the copies of the parents
			else if(SharedData.random.nextDouble() < SharedData.MUTATION_PROB){
				child1.mutate();
			}
			else if(SharedData.random.nextDouble() < SharedData.MUTATION_PROB){
				child2.mutate();
			}
			

			//add the new children to the new population
			newPopulation.add(child1);
			newPopulation.add(child2);
		}
		

		//calculate fitness of the new population chromosomes 
		for(int i=0;i<newPopulation.size();i++){
			newPopulation.get(i).calculateFitness(SharedData.EVALUATION_TIME);
			if(newPopulation.get(i).getConstrainFitness() < 1){
				System.out.println("\tChromosome #" + (i+1) + " Constrain Fitness: " + newPopulation.get(i).getConstrainFitness());
			}
			else{
				System.out.println("\tChromosome #" + (i+1) + " Fitness: " + newPopulation.get(i).getFitness());
			}
		}
		

		//add the best chromosome(s) from old population to the new population
		Collections.sort(newPopulation);
		for(int i=SharedData.POPULATION_SIZE - SharedData.ELITISM_NUMBER;i<newPopulation.size();i++){
			newPopulation.remove(i);
		}

		if(fPopulation.isEmpty()){
			Collections.sort(iPopulation);
			for(int i=0;i<SharedData.ELITISM_NUMBER;i++){
				newPopulation.add(iPopulation.get(i));
			}
		}
		else{
			Collections.sort(fPopulation);
			for(int i=0;i<SharedData.ELITISM_NUMBER;i++){
				newPopulation.add(fPopulation.get(i));
			}
		}
		
		return newPopulation;
	}
	/**
	 * Roullete wheel selection for the infeasible population
	 * @param population	array of chromosomes surviving in this population
	 * @return				the picked chromosome based on its constraint fitness
	 */
	private Chromosome constraintRouletteWheelSelection(ArrayList<Chromosome> population){
		//calculate the probabilities of the chromosomes based on their fitness
		double[] probabilities = new double[population.size()];
		probabilities[0] = population.get(0).getConstrainFitness();
		for(int i=1; i<population.size(); i++){
			probabilities[i] = probabilities[i-1] + population.get(i).getConstrainFitness() + SharedData.EIPSLON;
		}
		
		for(int i=0; i<probabilities.length; i++){
			probabilities[i] = probabilities[i] / probabilities[probabilities.length - 1];
		}
		

		//choose a chromosome based on its probability
		double prob = SharedData.random.nextDouble();
		for(int i=0; i<probabilities.length; i++){
			if(prob < probabilities[i]){
				return population.get(i);
			}
		}
		
		return population.get(0);
	}
	
	/**
	 * Get the fitness for any population
	 * @param population	array of chromosomes surviving in this population
	 * @return				the picked chromosome based on its fitness
	 */
	private Chromosome rouletteWheelSelection(ArrayList<Chromosome> population){
		//if the population is infeasible use the constraintRoulletWheel function
		if(population.get(0).getConstrainFitness() < 1){
			return constraintRouletteWheelSelection(population);
		}
		

		//calculate the probabilities for the current population
		double[] probabilities = new double[population.size()];
		probabilities[0] = population.get(0).getCombinedFitness();
		for(int i=1; i<population.size(); i++){
			probabilities[i] = probabilities[i-1] + population.get(i).getCombinedFitness() + SharedData.EIPSLON;
		}
		
		for(int i=0; i<probabilities.length; i++){
			probabilities[i] = probabilities[i] / probabilities[probabilities.length - 1];
		}

		//choose random chromosome based on its fitness
		double prob = SharedData.random.nextDouble();
		for(int i=0; i<probabilities.length; i++){
			if(prob < probabilities[i]){
				return population.get(i);
			}
		}
		
		return population.get(0);
	}
	@Override
	public String[][] generateRules(SLDescription sl, ElapsedCpuTimer time) {
		//initialize the statistics objects
		bestFitness = new ArrayList<Double>();
		numOfFeasible = new ArrayList<Integer>();
		numOfInFeasible = new ArrayList<Integer>();
		
		System.out.println("Generation #0: ");
		ArrayList<Chromosome> fChromosomes = new ArrayList<Chromosome>();
		ArrayList<Chromosome> iChromosomes = new ArrayList<Chromosome>();
		
		randomGen = new ruleGenerators.randomRuleGenerator.RuleGenerator(sl, time);
		constructGen = new ruleGenerators.constructiveRuleGenerator.RuleGenerator(sl, time);
//		for(int i = 0; i < SharedData.POPULATION_SIZE / 2; i++) {
//			Chromosome c = new Chromosome(randomGen.generateRules(sl, time), sl, time, usefulSprites);
//
//			c.calculateFitness(SharedData.EVALUATION_TIME);
//			if(c.getConstrainFitness() < 1){
//				iChromosomes.add(c);
//				System.out.println("\tChromosome #" + (i+1) + " Constrain Fitness: " + c.getConstrainFitness());
//			}
//			else{
//				fChromosomes.add(c);
//				System.out.println("\tChromosome #" + (i+1) + " Fitness: " + c.getFitness());
//			}
//			System.out.println("Here 1");
//		}
		for(int i = 0; i < SharedData.POPULATION_SIZE; i++) {
			Chromosome c = new Chromosome(constructGen.generateRules(sl, time), sl, time, usefulSprites);

			c.calculateFitness(SharedData.EVALUATION_TIME);
			if(c.getConstrainFitness() < 1){
				iChromosomes.add(c);
				System.out.println("\tChromosome #" + (i+1) + " Constrain Fitness: " + c.getConstrainFitness());
			}
			else{
				fChromosomes.add(c);
				System.out.println("\tChromosome #" + (i+1) + " Fitness: " + c.getFitness());
			}
			System.out.println("Here 2");
		}
		
		initPop(sl, time);

		//some variables to make sure not getting out of time
		double worstTime = SharedData.EVALUATION_TIME * SharedData.POPULATION_SIZE;
		double avgTime = worstTime;
		double totalTime = 0;
		int numberOfIterations = 0;
		System.out.println(time.remainingTimeMillis() + " avgTime: " + avgTime + " worstTime: " + worstTime);
		while(time.remainingTimeMillis() > 2 * avgTime &&
				time.remainingTimeMillis() > worstTime){
			ElapsedCpuTimer timer = new ElapsedCpuTimer();
			
			System.out.println("Generation #" + (numberOfIterations + 2) + ": ");
			

			//get the new population and split it to a the feasible and infeasible populations
			ArrayList<Chromosome> chromosomes = getNextPopulation(fChromosomes, iChromosomes);
			fChromosomes.clear();
			iChromosomes.clear();
			for(Chromosome c:chromosomes){
				if(c.getConstrainFitness() < 1){
					iChromosomes.add(c);
				}
				else{
					fChromosomes.add(c);
				}
			}
			
			numberOfIterations += 1;
			totalTime += timer.elapsedMillis();
			avgTime = totalTime / numberOfIterations;
		}
		

		//return the best infeasible chromosome
		if(fChromosomes.isEmpty()){
			for(int i=0;i<iChromosomes.size();i++){
				iChromosomes.get(i).calculateFitness(SharedData.EVALUATION_TIME);
			}

			Collections.sort(iChromosomes);
			bestChromosomeRuleset = iChromosomes.get(0).getRuleset();
			System.out.println("Best Fitness: " + iChromosomes.get(0).getConstrainFitness());
			return iChromosomes.get(0).getRuleset();
		}
		
		//return the best feasible chromosome otherwise and print some statistics
		for(int i=0;i<fChromosomes.size();i++){
			fChromosomes.get(i).calculateFitness(SharedData.EVALUATION_TIME);
		}
		Collections.sort(fChromosomes);
		bestChromosomeRuleset = fChromosomes.get(0).getRuleset();
		System.out.println("Best Chromosome Fitness: " + fChromosomes.get(0).getFitness());
		System.out.println(bestFitness);
		System.out.println(numOfFeasible);
		System.out.println(numOfInFeasible);
		return fChromosomes.get(0).getRuleset();
	}
	
	/**
	 * 
	 */
	
	
}
