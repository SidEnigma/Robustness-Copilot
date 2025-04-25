package org.openscience.cdk.formula;
 
 import org.openscience.cdk.interfaces.IChemObjectBuilder;
 import org.openscience.cdk.interfaces.IIsotope;
 import org.openscience.cdk.interfaces.IMolecularFormula;
 import org.openscience.cdk.interfaces.IMolecularFormulaSet;
 
 /**
  * This class generates molecular formulas within given mass range and elemental
  * composition.
  *
  * Usage:
  *
  * <pre>
  * IsotopeFactory ifac = Isotopes.getInstance();
  * IIsotope c = ifac.getMajorIsotope(&quot;C&quot;);
  * IIsotope h = ifac.getMajorIsotope(&quot;H&quot;);
  * IIsotope n = ifac.getMajorIsotope(&quot;N&quot;);
  * IIsotope o = ifac.getMajorIsotope(&quot;O&quot;);
  * IIsotope p = ifac.getMajorIsotope(&quot;P&quot;);
  * IIsotope s = ifac.getMajorIsotope(&quot;S&quot;);
  *
  * MolecularFormulaRange mfRange = new MolecularFormulaRange();
  * mfRange.addIsotope(c, 0, 50);
  * mfRange.addIsotope(h, 0, 100);
  * mfRange.addIsotope(o, 0, 50);
  * mfRange.addIsotope(n, 0, 50);
  * mfRange.addIsotope(p, 0, 10);
  * mfRange.addIsotope(s, 0, 10);
  *
  * MolecularFormulaGenerator mfg = new MolecularFormulaGenerator(builder, minMass,
  *         maxMass, mfRange);
  * double minMass = 133.003;
  * double maxMass = 133.005;
  * IMolecularFormulaSet mfSet = mfg.getAllFormulas();
  * </pre>
  *
  * This class offers two implementations: The Round Robin algorithm {@cdk.cite Boecker2008} on mass ranges
  * {@cdk.cite Duehrkop2013} is used on most inputs. For special cases (e.g. single elements, extremely large mass ranges)
  * a full enumeration algorithm {@cdk.cite Pluskal2012} is used.
  *
  * The Round Robin algorithm was originally developed for the SIRIUS 3 software. The full enumeration algorithm was
  * originally developed for a MZmine 2 framework module, published in Pluskal et al. {@cdk.cite Pluskal2012}.
  *
  * @cdk.module formula
  * @author Tomas Pluskal, Kai Dührkop, Marcus Ludwig
  * @cdk.created 2014-12-28
  * @cdk.githash
  */
 public class MolecularFormulaGenerator implements IFormulaGenerator {
 
     /**
      * The chosen implementation
      */
     protected final IFormulaGenerator formulaGenerator;
 
     /**
      * Initiate the MolecularFormulaGenerator.
      *
      * @param minMass
      *            Lower boundary of the target mass range
      * @param maxMass
      *            Upper boundary of the target mass range
      * @param mfRange
      *            A range of elemental compositions defining the search space
      * @throws IllegalArgumentException
      *             In case some of the isotopes in mfRange has undefined exact
      *             mass or in case illegal parameters are provided (e.g.,
      *             negative mass values or empty MolecularFormulaRange)
      * @see MolecularFormulaRange
      */
     public MolecularFormulaGenerator(final IChemObjectBuilder builder,
                                      final double minMass, final double maxMass,
                                      final MolecularFormulaRange mfRange) {
         checkInputParameters(builder,minMass,maxMass,mfRange);
         this.formulaGenerator = isIllPosed(minMass, maxMass, mfRange) ? new FullEnumerationFormulaGenerator(builder, minMass, maxMass, mfRange) : new RoundRobinFormulaGenerator(builder, minMass, maxMass, mfRange);
     }
 
 
/** Decides to use the round robin algorithm or the full enumeration algorithm. */
 private static boolean isIllPosed(double minMass, double maxMass, MolecularFormulaRange mfRange){}

 

}