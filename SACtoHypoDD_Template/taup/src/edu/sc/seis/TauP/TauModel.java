/*
 * The TauP Toolkit: Flexible Seismic Travel-Time and Raypath Utilities.
 * Copyright (C) 1998-2000 University of South Carolina This program is free
 * software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple Place -
 * Suite 330, Boston, MA 02111-1307, USA. The current version can be found at <A
 * HREF="www.seis.sc.edu">http://www.seis.sc.edu </A> Bug reports and comments
 * should be directed to H. Philip Crotwell, crotwell@seis.sc.edu or Tom Owens,
 * owens@seis.sc.edu
 */
package edu.sc.seis.TauP;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.io.StreamTokenizer;

/**
 * provides storage all of the TauBranch's comprising a model.
 * 
 * @version 1.1.3 Wed Jul 18 15:00:35 GMT 2001
 * @author H. Philip Crotwell
 */
public class TauModel implements Serializable, Cloneable {

    /** True to enable debugging output. */
    transient public boolean DEBUG = false;

    /** True if this is a spherical slowness model. False if flat. */
    public boolean spherical = true;

    /** Depth for which tau model was constructed. */
    protected double sourceDepth = 0.0;

    /** Branch with the source at its top. */
    protected int sourceBranch = 0;

    /**
     * Depths that should not have reflections or phase conversions. For
     * instance, if the source is not at a branch boundary then noDisconDepths
     * contains source depth and reflections and phase conversions are not
     * allowed at this branch boundary. If the source happens to fall on a real
     * discontinuity then then it is not included.
     */
    protected double[] noDisconDepths = new double[0];

    /** Depth of the moho. */
    protected double mohoDepth;

    /** Branch with the moho at its top. */
    protected int mohoBranch;

    /** Depth of the cmb. */
    protected double cmbDepth;

    /** Branch with the cmb at its top. */
    protected int cmbBranch;

    /** Depth of the iocb. */
    protected double iocbDepth;

    /** Branch with the iocb at its top. */
    protected int iocbBranch;

    /** Radius of the Earth in km, usually input from the velocity model. */
    protected double radiusOfEarth = 6371.0;

    /**
     * The slowness model that was used to generate the tau model. This in
     * needed in order to modify the tau branches from a surface focus event to
     * an event at depth. This is normally be set when the tau model is
     * generated to be a clone of the slowness model.
     */
    public SlownessModel sMod;

    /**
     * ray parameters used to construct the tau branches. This may only be a
     * subset of the slownesses/ray parameters saved in the slowness model due
     * to high slowness zones (low velocity zones).
     */
    protected double[] rayParams;

    /**
     * 2D Array containing a TauBranch object corresponding to each "branch" of
     * the tau model, 0 is P and 1 is S. Branches correspond to depth regions
     * between discontinuities or reversals in slowness gradient for a wave
     * type. Each branch contains time, distance, and tau increments for each
     * ray parameter in rayParams for the layer. Rays that turn above the branch
     * layer are assigned 0.0 time, distance, and tau increments.
     */
    public TauBranch[][] tauBranches = new TauBranch[2][];

    // Methods -----------------------------------------------------------
    // accessor methods
    /** @returns the name of the earth model used to construct the tau model. */
    public String getModelName() {
        return sMod.vMod.getModelName();
    }

    public SlownessModel getSlownessModel() {
        return sMod;
    }

    public VelocityModel getVelocityModel() {
        return sMod.vMod;
    }

    /** @returns depth for which tau model was constructed. */
    public double getSourceDepth() {
        return sourceDepth;
    }

    /** @returns branch number with the source at its top. */
    public int getSourceBranch() {
        return sourceBranch;
    }

    /**
     * Branches, such as the branch with the source at its top, that are not
     * allowed to have reflections and phase conversions at their tops.
     */
    public double[] getNoDisconDepths() {
        return noDisconDepths;
    }

    /**
     * Does the given branch number have a noDisconDepth at its top? We test
     * against PWave Tau branches (ie true) since S is the same.
     */
    public boolean isNoDisconBranch(int branchNum) {
        for(int i = 0; i < noDisconDepths.length; i++) {
            if(noDisconDepths[i] == getTauBranch(branchNum, true).getTopDepth()) { return true; }
        }
        return false;
    }

    /**
     * Is the given depth a "noDisconDepth"?
     */
    public boolean isNoDisconDepth(double noDisconDepth) {
        for(int i = 0; i < noDisconDepths.length; i++) {
            if(noDisconDepths[i] == noDisconDepth) { return true; }
        }
        return false;
    }

    public synchronized void setNoDisconDepths(double[] noDisconDepths) {
        this.noDisconDepths = noDisconDepths;
    }

    public synchronized void appendNoDisconDepth(double noDisconDepth) {
        double[] temp = new double[noDisconDepths.length + 1];
        System.arraycopy(noDisconDepths, 0, temp, 0, noDisconDepths.length);
        noDisconDepths = temp;
        noDisconDepths[noDisconDepths.length - 1] = noDisconDepth;
    }

    /** @returns depth of the moho. */
    public double getMohoDepth() {
        return mohoDepth;
    }

    /** @returns branch number with the moho at its top. */
    public int getMohoBranch() {
        return mohoBranch;
    }

    /** @returns depth of the cmb. */
    public double getCmbDepth() {
        return cmbDepth;
    }

    /** @returns branch number with the cmb at its top. */
    public int getCmbBranch() {
        return cmbBranch;
    }

    /** @returns depth of the iocb. */
    public double getIocbDepth() {
        return iocbDepth;
    }

    /** @returns branch number with the iocb at its top. */
    public int getIocbBranch() {
        return iocbBranch;
    }

    /**
     * @returns the radius of the Earth in km, usually input from the velocity
     *          model.
     */
    public double getRadiusOfEarth() {
        return radiusOfEarth;
    }

    /**
     * @returns an array, cloned, of the ray parameters used to construct the
     *          tau branches. This may only be a subset of the slownesses/ray
     *          parameters saved in the slowness model due to high slowness
     *          zones (low velocity zones).
     */
    public double[] getRayParams() {
        return (double[])rayParams.clone();
    }

    public double getOneRayParam(int i) {
        return rayParams[i];
    }

    public int getNumBranches() {
        return tauBranches[0].length;
    }

    public TauBranch getTauBranch(int branchNum, boolean isPWave) {
        if(isPWave) {
            return tauBranches[0][branchNum];
        } else {
            return tauBranches[1][branchNum];
        }
    }

    /**
     * returns an array of the depths that are boundaries between branches
     */
    public double[] getBranchDepths() {
        double[] branchDepths = new double[getNumBranches()];
        // true means use p wave, but S wave should be the same
        branchDepths[0] = getTauBranch(0, true).getTopDepth();
        for(int i = 1; i < branchDepths.length; i++) {
            branchDepths[i] = getTauBranch(i - 1, true).getBotDepth();
        }
        return branchDepths;
    }

    /**
     * returns the turning depth for a ray of given ray parameter. Note this is
     * for a surface source, and so converted phases my give incorrect results,
     * e.g. SKS for certain ray parameters turns within the upper part of the
     * outer core that is a low velocity zone for P so no P wave of that ray
     * parameter could reach the core. For layer specific turning points, see
     * the other SlownessModel.findDepth.
     */
    public double findDepth(double rayParam, boolean isPWave)
            throws TauModelException {
        try {
            return sMod.findDepth(rayParam, isPWave);
        } catch(SlownessModelException e) {
            throw new TauModelException("findDepth: caught SlownessModelException:"
                    + e.getMessage());
        }
    }

    // normal methods
    /**
     * Calculates tau for each branch within a slowness model.
     * 
     * @exception SlownessModelException
     *                occurs if getNumLayers() < 1 as we cannot compute a
     *                distance without a layer.
     */
    public void calcTauIncFrom(SlownessModel sMod)
            throws SlownessModelException, NoSuchLayerException,
            TauModelException, NoSuchMatPropException {
        SlownessLayer topSLayer, botSLayer, currSLayer;
        TimeDist timedist = new TimeDist();
        int topCritLayerNum, botCritLayerNum;
        /*
         * First, we must have at least 1 slowness layer to calculate a
         * distance. Otherwise we must signal an exception.
         */
        if(DEBUG) {
            System.out.println("Size of slowness model:"
                    + " sMod.getNumLayers('P') = " + sMod.getNumLayers(true)
                    + ", sMod.getNumLayers('S') = " + sMod.getNumLayers(false));
        }
        if(sMod.getNumLayers(true) == 0 || sMod.getNumLayers(false) == 0) { throw new SlownessModelException("Can't calculate tauInc when getNumLayers() = 0. "
                + "I need more slowness samples."); }
        if(!sMod.validate()) { throw new SlownessModelException("Validation failed: "
                + "Something is wrong with the slowness model."); }
        this.sMod = (SlownessModel)sMod.clone();
        radiusOfEarth = sMod.getRadiusOfEarth();
        sourceDepth = 0.0;
        sourceBranch = 0;
        /*
         * Create a array holding the ray parameter that we will use for
         * constructing the tau splines. Only store ray parameters that are not
         * in a high slowness zone, ie they are smaller than the minimum ray
         * parameter seen so far.
         */
        int numBranches = sMod.getNumCriticalDepths() - 1;
        tauBranches[0] = new TauBranch[numBranches];
        tauBranches[1] = new TauBranch[numBranches];
        /*
         * Here we find the list of ray parameters to be used for the tau model.
         * We only need to find ray parameters for S waves since P waves have
         * been constructed to be a subset of the S samples.
         */
        int rayNum = 0;
        double minPSoFar = sMod.getSlownessLayerClone(0, false).getTopP();
        double[] tempRayParams = new double[2 * sMod.getNumLayers(false)
                + sMod.getNumCriticalDepths()];
        // make sure we get the top slowness of the very top layer
        tempRayParams[rayNum] = minPSoFar;
        rayNum++;
        for(int layerNum = 0; layerNum < sMod.getNumLayers(false); layerNum++) {
            currSLayer = sMod.getSlownessLayer(layerNum, false);
            /*
             * Add the top if it is strictly less than the last sample added.
             * Note that this will not be added if the slowness is continuous
             * across the layer boundary.
             */
            if(currSLayer.getTopP() < minPSoFar) {
                tempRayParams[rayNum] = currSLayer.getTopP();
                rayNum++;
                minPSoFar = currSLayer.getTopP();
            }
            /*
             * Add the bottom if it is strictly less than the last sample added.
             * This will always happen unless we are within a high slowness
             * zone.
             */
            if(currSLayer.getBotP() < minPSoFar) {
                tempRayParams[rayNum] = currSLayer.getBotP();
                rayNum++;
                minPSoFar = currSLayer.getBotP();
            }
        }
        /* Copy tempRayParams to rayParams so the the size is exactly right. */
        rayParams = new double[rayNum];
        System.arraycopy(tempRayParams, 0, rayParams, 0, rayNum);
        tempRayParams = null;
        if(DEBUG) {
            System.out.println("Number of slowness samples for tau =" + rayNum);
        }
        CriticalDepth topCritDepth, botCritDepth;
        int waveNum;
        boolean isPWave;
        for(waveNum = 0, isPWave = true; waveNum < 2; waveNum++, isPWave = false) {
            // The minimum slowness seen so far
            minPSoFar = sMod.getSlownessLayerClone(0, isPWave).getTopP();
            // loop over critical slowness layers since they form the branches
            for(int critNum = 0; critNum < sMod.getNumCriticalDepths() - 1; critNum++) {
                topCritDepth = sMod.getCriticalDepth(critNum);
                topCritLayerNum = topCritDepth.getLayerNum(isPWave);
                botCritDepth = sMod.getCriticalDepth(critNum + 1);
                botCritLayerNum = botCritDepth.getLayerNum(isPWave) - 1;
                if(DEBUG) {
                    System.out.println("Calculating " + (isPWave ? "P" : "S")
                            + " tau branch for branch " + critNum
                            + " topCritLayerNum=" + topCritLayerNum
                            + " botCritLayerNum=" + botCritLayerNum
                            + "\nminPSoFar=" + minPSoFar);
                }
                tauBranches[waveNum][critNum] = new TauBranch(topCritDepth.depth,
                                                              botCritDepth.depth,
                                                              isPWave);
                tauBranches[waveNum][critNum].DEBUG = DEBUG;
                tauBranches[waveNum][critNum].createBranch(sMod,
                                                           minPSoFar,
                                                           rayParams);
                /*
                 * update minPSoFar. Note that the new minPSoFar could be at the
                 * start of a discontinuty over a high slowness zone, so we need
                 * to check the top, bottom and the layer just above the
                 * discontinuity.
                 */
                topSLayer = sMod.getSlownessLayer(topCritLayerNum, isPWave);
                botSLayer = sMod.getSlownessLayer(botCritLayerNum, isPWave);
                minPSoFar = Math.min(minPSoFar, Math.min(topSLayer.getTopP(),
                                                         botSLayer.getBotP()));
                botSLayer = sMod.getSlownessLayer(sMod.layerNumberAbove(botCritDepth.depth,
                                                                        isPWave),
                                                  isPWave);
                minPSoFar = Math.min(minPSoFar, botSLayer.getBotP());
            }
        }
        /*
         * Here we decide which branches are the closest to the moho, cmb, and
         * iocb by comparing the depth of the top of the branch with the depths
         * in the Velocity Model.
         */
        double bestMoho = Double.MAX_VALUE;
        double bestCmb = Double.MAX_VALUE;
        double bestIocb = Double.MAX_VALUE;
        for(int branchNum = 0; branchNum < tauBranches[0].length; branchNum++) {
            TauBranch tBranch = (TauBranch)tauBranches[0][branchNum];
            if(Math.abs(tBranch.getTopDepth() - sMod.vMod.getMohoDepth()) <= bestMoho) {
                mohoBranch = branchNum;
                bestMoho = Math.abs(tBranch.getTopDepth()
                        - sMod.vMod.getMohoDepth());
            }
            if(Math.abs(tBranch.getTopDepth() - sMod.vMod.getCmbDepth()) < bestCmb) {
                cmbBranch = branchNum;
                bestCmb = Math.abs(tBranch.getTopDepth()
                        - sMod.vMod.getCmbDepth());
            }
            if(Math.abs(tBranch.getTopDepth() - sMod.vMod.getIocbDepth()) < bestIocb) {
                iocbBranch = branchNum;
                bestIocb = Math.abs(tBranch.getTopDepth()
                        - sMod.vMod.getIocbDepth());
            }
        }
        /*
         * Now set mohoDepth, etc to the top of the branches we have decided on.
         */
        mohoDepth = tauBranches[0][mohoBranch].getTopDepth();
        cmbDepth = tauBranches[0][cmbBranch].getTopDepth();
        iocbDepth = tauBranches[0][iocbBranch].getTopDepth();
        if(!validate()) { throw new TauModelException("calcTauIncFrom: Validation failed!"); }
    }

    /**
     * Finds the branch that either has the depth as its top boundary, or
     * strictly contains the depth. Also, we allow the bottommost branch to
     * contain its bottom depth, so that the center if the earth is contained
     * within the bottom branch.
     */
    public int findBranch(double depth) throws TauModelException {
        for(int i = 0; i < tauBranches[0].length; i++) {
            if(tauBranches[0][i].getTopDepth() <= depth
                    && tauBranches[0][i].getBotDepth() > depth) { return i; }
        }
        /* Check to see if depth is center of earth. */
        if(tauBranches[0][tauBranches[0].length - 1].getBotDepth() == depth) {
            return tauBranches[0].length - 1;
        } else {
            throw new TauModelException("No TauBranch contains depth=" + depth);
        }
    }

    /**
     * Computes a new tau model for a source at depth using the previously
     * computed branches for a surface source. No change is needed to the
     * branches above and below the branch containing the depth, except for the
     * addition of a slowness sample. The branch containing the source depth is
     * split into 2 branches, and up going branch and a downgoing branch.
     * Additionally, the slowness at the source depth must be sampled exactly as
     * it is an extremal point for each of these branches. See Buland and
     * Chapman p 1290.
     */
    public TauModel depthCorrect(double depth) throws TauModelException {
        if(sourceDepth != 0.0) { throw new TauModelException("depthCorrect: Can't depth correct "
                + "a tau model that is not for a surface source."); }
        if(depth > getCmbDepth()) { throw new TauModelException("depthCorrect: Can't depth correct "
                + "for a depth in the core."); }
        TauModel tMod = splitBranch(depth);
        tMod.sourceDepth = depth;
        tMod.sourceBranch = tMod.findBranch(depth);
        validate();
        return tMod;
    }

    /**
     * returns a new TauModel with the branches containing depth split at depth.
     * Used for putting a source at depth since a source can only be located on
     * a branch boundary.
     */
    public TauModel splitBranch(double depth) throws TauModelException {
        TauModel tMod;
        int topCritLayerNum, botCritLayerNum;
        SlownessLayer topSLayer, botSLayer;
        try {
            tMod = (TauModel)clone();
            /*
             * first check to see if depth is already a branch boundary. If so
             * then we need only return the clone.
             */
            for(int branchNum = 0; branchNum < tMod.tauBranches[0].length; branchNum++) {
                if(tMod.tauBranches[0][branchNum].getTopDepth() == depth
                        || tMod.tauBranches[0][branchNum].getBotDepth() == depth) { return tMod; }
            }
            /*
             * depth is not a branch boundary, so we must modify the tau model.
             */
            int indexP = -1;
            double PWaveRayParam = -1.0;
            int indexS = -1;
            double SWaveRayParam = -1.0;
            int waveNum;
            boolean isPWave;
            SplitLayerInfo splitInfo;
            /*
             * do S wave first (isPWave=false) since the S wave ray parameter is >
             * P wave ray parameter and thus comes before it in the rayParams
             * array
             */
            for(waveNum = 1, isPWave = false; waveNum >= 0; waveNum--, isPWave = true) {
                splitInfo = tMod.sMod.splitLayer(depth, isPWave);
                if(splitInfo.movedSample) {} else if(splitInfo.neededSplit) {
                    /*
                     * We split the slowness layers containing depth into 2
                     * layers each.
                     */
                    int layerNum = tMod.sMod.layerNumberAbove(depth, isPWave);
                    SlownessLayer sLayer = tMod.sMod.getSlownessLayer(layerNum,
                                                                      isPWave);
                    double newRayParam = splitInfo.getRayParam();
                    int index = -1;
                    // add the new ray parameters to the rayParams array
                    // Only loop to length-1 as last sample is always 0
                    // and negative is not allowed
                    for(int i = 0; i < tMod.rayParams.length - 1; i++) {
                        if(tMod.rayParams[i] < newRayParam
                                && tMod.rayParams[i + 1] > newRayParam) {
                            index = i;
                            double[] oldRayParams = tMod.rayParams;
                            tMod.rayParams = new double[oldRayParams.length + 1];
                            System.arraycopy(oldRayParams,
                                             0,
                                             tMod.rayParams,
                                             0,
                                             index);
                            tMod.rayParams[index] = newRayParam;
                            System.arraycopy(oldRayParams,
                                             index,
                                             tMod.rayParams,
                                             index + 1,
                                             oldRayParams.length - index);
                            if(isPWave) {
                                indexP = index;
                                PWaveRayParam = newRayParam;
                            } else {
                                indexS = index;
                                SWaveRayParam = newRayParam;
                            }
                            break;
                        }
                    }
                }
            }
            /*
             * Now we add a sample to each branch above the depth, split the
             * branch containing the depth, and add a sample to each deeper
             * branch.
             */
            int branchToSplit = tMod.findBranch(depth);
            int topCritLayerNumP, botCritLayerNumP;
            int topCritLayerNumS, botCritLayerNumS;
            int splitLayerNumP, splitLayerNumS;
            TauBranch[][] newtauBranches = new TauBranch[2][tMod.getNumBranches() + 1];
            for(int i = 0; i < branchToSplit; i++) {
                newtauBranches[0][i] = (TauBranch)tMod.tauBranches[0][i].clone();
                newtauBranches[1][i] = (TauBranch)tMod.tauBranches[1][i].clone();
                topCritLayerNumP = tMod.sMod.layerNumberBelow(newtauBranches[0][i].getTopDepth(),
                                                              true);
                botCritLayerNumP = tMod.sMod.layerNumberAbove(newtauBranches[0][i].getBotDepth(),
                                                              true);
                topCritLayerNumS = tMod.sMod.layerNumberBelow(newtauBranches[1][i].getTopDepth(),
                                                              false);
                botCritLayerNumS = tMod.sMod.layerNumberAbove(newtauBranches[1][i].getBotDepth(),
                                                              false);
                if(indexS != -1) {
                    // add the new ray parameter from splitting the S Wave
                    // slowness layer to both the P and S wave Tau branches
                    newtauBranches[0][i].insert(SWaveRayParam,
                                                tMod.sMod,
                                                indexS);
                    newtauBranches[1][i].insert(SWaveRayParam,
                                                tMod.sMod,
                                                indexS);
                }
                if(indexP != -1) {
                    // add the new ray parameter from splitting the P Wave
                    // slowness layer to both the P and S wave Tau branches
                    newtauBranches[0][i].insert(PWaveRayParam,
                                                tMod.sMod,
                                                indexP);
                    newtauBranches[1][i].insert(PWaveRayParam,
                                                tMod.sMod,
                                                indexP);
                }
            }
            tMod.appendNoDisconDepth(depth);
            topCritLayerNumS = tMod.sMod.layerNumberBelow(tMod.tauBranches[1][branchToSplit].getTopDepth(),
                                                          false);
            splitLayerNumS = tMod.sMod.layerNumberAbove(depth, false);
            newtauBranches[1][branchToSplit] = new TauBranch(tMod.tauBranches[1][branchToSplit].getTopDepth(),
                                                             depth,
                                                             false);
            newtauBranches[1][branchToSplit].createBranch(tMod.sMod,
                                                          tMod.tauBranches[1][branchToSplit].getMaxRayParam(),
                                                          tMod.rayParams);
            newtauBranches[1][branchToSplit + 1] = tMod.tauBranches[1][branchToSplit].difference(newtauBranches[1][branchToSplit],
                                                                                                 indexP,
                                                                                                 indexS,
                                                                                                 tMod.sMod,
                                                                                                 newtauBranches[1][branchToSplit].getMinRayParam(),
                                                                                                 tMod.rayParams);
            topCritLayerNumP = tMod.sMod.layerNumberBelow(tMod.tauBranches[0][branchToSplit].getTopDepth(),
                                                          true);
            splitLayerNumP = tMod.sMod.layerNumberAbove(depth, true);
            newtauBranches[0][branchToSplit] = new TauBranch(tMod.tauBranches[0][branchToSplit].getTopDepth(),
                                                             depth,
                                                             true);
            newtauBranches[0][branchToSplit].createBranch(tMod.sMod,
                                                          tMod.tauBranches[0][branchToSplit].getMaxRayParam(),
                                                          tMod.rayParams);
            newtauBranches[0][branchToSplit + 1] = tMod.tauBranches[0][branchToSplit].difference(newtauBranches[0][branchToSplit],
                                                                                                 indexP,
                                                                                                 indexS,
                                                                                                 tMod.sMod,
                                                                                                 newtauBranches[0][branchToSplit].getMinRayParam(),
                                                                                                 tMod.rayParams);
            for(int i = branchToSplit + 1; i < tMod.tauBranches[0].length; i++) {
                newtauBranches[1][i + 1] = tMod.tauBranches[1][i];
                newtauBranches[0][i + 1] = tMod.tauBranches[0][i];
                if(indexS != -1) {
                    // add the new ray parameter from splitting the S Wave
                    // slowness layer to both the P and S wave Tau branches
                    newtauBranches[0][i + 1].insert(SWaveRayParam,
                                                    tMod.sMod,
                                                    indexS);
                    newtauBranches[1][i + 1].insert(SWaveRayParam,
                                                    tMod.sMod,
                                                    indexS);
                }
                if(indexP != -1) {
                    // add the new ray parameter from splitting the P Wave
                    // slowness layer to both the P and S wave Tau branches
                    newtauBranches[0][i + 1].insert(PWaveRayParam,
                                                    tMod.sMod,
                                                    indexP);
                    newtauBranches[1][i + 1].insert(PWaveRayParam,
                                                    tMod.sMod,
                                                    indexP);
                }
            }
            tMod.tauBranches = newtauBranches;
            /*
             * We have split a branch so possibly sourceBranch, mohoBranch,
             * cmbBranch, and iocbBranch are off by 1.
             */
            if(tMod.sourceDepth > depth) tMod.sourceBranch++;
            if(tMod.mohoDepth > depth) tMod.mohoBranch++;
            if(tMod.cmbDepth > depth) tMod.cmbBranch++;
            if(tMod.iocbDepth > depth) tMod.iocbBranch++;
            if(!tMod.validate()) { throw new TauModelException("splitBranch("
                    + depth + "): Validation failed!"); }
        } catch(NoSuchLayerException e) {
            throw new TauModelException("TauModel.depthCorrect - "
                    + "NoSuchLayerException", e);
        } catch(SlownessModelException e) {
            e.printStackTrace();
            throw new TauModelException("TauModel.depthCorrect - "
                    + "SlownessModelException", e);
        } finally {}
        return tMod;
    }

    public void writeModel(String filename) throws IOException {
        FileOutputStream fOut = new FileOutputStream(filename);
        ObjectOutputStream out = new ObjectOutputStream(fOut);
        try {
            out.writeObject(this);
        } finally {
            out.close();
            fOut.close();
        }
    }

    public void writeModelToStream(OutputStream outStream) throws IOException {
        ObjectOutputStream out = new ObjectOutputStream(outStream);
        out.writeObject(this);
    }

    public static TauModel readModel(String filename)
            throws FileNotFoundException, IOException,
            StreamCorruptedException, ClassNotFoundException,
            OptionalDataException {
        TauModel tMod;
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(filename));
        try {
            tMod = readModelFromStream(in);
        } finally {
            in.close();
        }
        return tMod;
    }

    public static TauModel readModelFromStream(InputStream inStream)
            throws InvalidClassException, IOException,
            StreamCorruptedException, ClassNotFoundException,
            OptionalDataException {
        ObjectInputStream in = new ObjectInputStream(inStream);
        TauModel tMod = (TauModel)in.readObject();
        return tMod;
    }

    /*
     * public void writeToStream(String filename) throws IOException {
     * DataOutputStream dos = new DataOutputStream( new BufferedOutputStream(
     * new FileOutputStream(filename))); writeToStream(dos); dos.close(); }
     * public void writeToStream(DataOutputStream dos) throws IOException {
     * dos.writeInt(getClass().getName().length());
     * dos.writeBytes(getClass().getName()); dos.writeBoolean(spherical);
     * dos.writeDouble(sourceDepth); dos.writeInt(sourceBranch);
     * dos.writeInt(noDisconBranch); dos.writeDouble(mohoDepth);
     * dos.writeInt(mohoBranch); dos.writeDouble(cmbDepth);
     * dos.writeInt(cmbBranch); dos.writeDouble(iocbDepth);
     * dos.writeInt(iocbBranch); dos.writeDouble(radiusOfEarth);
     * sMod.writeToStream(dos); dos.writeInt(rayParams.length); for (int i=0;i
     * <rayParams.length;i++) { dos.writeDouble(rayParams[i]); }
     * dos.writeInt(getNumBranches()); for (int i=0;i <getNumBranches();i++) {
     * tauBranches[0][i].writeToStream(dos);
     * tauBranches[1][i].writeToStream(dos); } } public static TauModel
     * readFromStream(String filename) throws FileNotFoundException,
     * IOException, InstantiationException, IllegalAccessException,
     * ClassNotFoundException { DataInputStream dis = new DataInputStream( new
     * BufferedInputStream( new FileInputStream(filename))); TauModel tMod =
     * readFromStream(dis); dis.close(); return tMod; } public static TauModel
     * readFromStream(DataInputStream dis) throws IOException,
     * ClassNotFoundException, IllegalAccessException, InstantiationException {
     * int length; byte[] classString = new byte[dis.readInt()];
     * dis.read(classString); Class tModClass = Class.forName(new
     * String(classString)); TauModel tMod = (TauModel)tModClass.newInstance();
     * tMod.spherical = dis.readBoolean(); tMod.sourceDepth = dis.readDouble();
     * tMod.sourceBranch = dis.readInt(); tMod.noDisconBranch = dis.readInt();
     * tMod.mohoDepth = dis.readDouble(); tMod.mohoBranch = dis.readInt();
     * tMod.cmbDepth = dis.readDouble(); tMod.cmbBranch = dis.readInt();
     * tMod.iocbDepth = dis.readDouble(); tMod.iocbBranch = dis.readInt();
     * tMod.radiusOfEarth = dis.readDouble(); tMod.sMod =
     * SlownessModel.readFromStream(dis); length = dis.readInt(); tMod.rayParams =
     * new double[length]; for (int i=0;i <tMod.rayParams.length;i++) {
     * tMod.rayParams[i] = dis.readDouble(); } length = dis.readInt();
     * tMod.tauBranches = new TauBranch[2][length]; for (int i=0;i <length;i++) {
     * tMod.tauBranches[0][i] = TauBranch.readFromStream(dis);
     * tMod.tauBranches[1][i] = TauBranch.readFromStream(dis); } return tMod; }
     */
    public boolean validate() {
        for(int i = 0; i < rayParams.length - 1; i++) {
            if(rayParams[i + 1] >= rayParams[i]) {
                System.err.println("RayParams are not monotonically decreasing. "
                        + "rayParams["
                        + i
                        + "]="
                        + rayParams[i]
                        + " rayParams[" + (i + 1) + "]=" + rayParams[(i + 1)]);
                return false;
            }
        }
        if(tauBranches[0].length != tauBranches[1].length) {
            System.err.println("TauBranches for P and S are not equal. "
                    + tauBranches[0].length + " " + tauBranches[1].length);
            return false;
        }
        if(tauBranches[0][0].getTopDepth() != 0 || tauBranches[1][0].getTopDepth() != 0) {
            System.err.println("branch 0 topDepth != 0");
            return false;
        }
        // this is only for S wave (tauBranches[1][])
        if(tauBranches[1][0].getMaxRayParam() != rayParams[0]) {
            System.err.println("branch 0 maxRayParam != rayParams[0]");
            return false;
        }
        for(int i = 1; i < getNumBranches(); i++) {
            if(tauBranches[0][i].getTopDepth() != tauBranches[1][i].getTopDepth()) {
                System.err.println("branch " + i + " P topDepth != S topDepth");
                return false;
            }
            if(tauBranches[0][i].getBotDepth() != tauBranches[1][i].getBotDepth()) {
                System.err.println("branch " + i + " P botDepth != S botDepth");
                return false;
            }
            if(tauBranches[0][i].getTopDepth() != tauBranches[0][i - 1].getBotDepth()) {
                System.err.println("branch " + i + " topDepth != botDepth of "
                        + (i - 1));
                return false;
            }
            if(tauBranches[0][i].getMaxRayParam() != tauBranches[0][i - 1].getMinRayParam()) {
                System.err.println("branch " + i
                        + " P maxRayParam != minRayParam of " + (i - 1)
                        + "\nmaxRayParam=" + tauBranches[0][i].getMaxRayParam()
                        + "\nminRayParam=" + tauBranches[0][i - 1].getMinRayParam());
                return false;
            }
            if(tauBranches[1][i].getMaxRayParam() != tauBranches[1][i - 1].getMinRayParam()) {
                System.err.println("branch " + i
                        + " S maxRayParam != minRayParam of " + (i - 1)
                        + "\nmaxRayParam=" + tauBranches[1][i].getMaxRayParam()
                        + "\nminRayParam=" + tauBranches[1][i - 1].getMinRayParam()
                        + "\ndepth = " + tauBranches[1][i].getTopDepth());
                return false;
            }
        }
        if(tauBranches[0][getNumBranches() - 1].getMinRayParam() != 0) {
            System.err.println("branch tauBranches[0].length-1 minRayParam != 0");
            return false;
        }
        if(tauBranches[1][getNumBranches() - 1].getMinRayParam() != 0) {
            System.err.println("branch tauBranches[1].length-1 minRayParam != 0");
            return false;
        }
        return true;
    }

    public void print() {
        double deg, time;
        if(DEBUG) System.out.println("Starting print() in TauModel");
        System.out.println("Delta tau for each slowness sample and layer.");
        for(int j = 0; j < rayParams.length; j++) {
            deg = 0;
            time = 0;
            for(int i = 0; i < getNumBranches(); i++) {
                deg += tauBranches[0][i].getDist(j) * 180 / Math.PI;
                time += tauBranches[0][i].time[j];
                System.out.println(" i " + i + " j " + j + " rayParam "
                        + rayParams[j] + " tau " + tauBranches[0][i].tau[j]
                        + " time " + tauBranches[0][i].time[j] + " dist "
                        + tauBranches[0][i].getDist(j) + " degrees "
                        + (tauBranches[0][i].getDist(j) * 180 / Math.PI));
            }
            System.out.println();
            System.out.println("deg= " + deg + "  time=" + time);
        }
    }

    /**
     * Returns a clone of the tau model so that changes to the clone do not
     * affect the original.
     */
    public Object clone() {
        TauModel newObject;
        try {
            newObject = (TauModel)super.clone();
            newObject.rayParams = (double[])rayParams.clone();
            newObject.sMod = (SlownessModel)sMod.clone();
            newObject.tauBranches = new TauBranch[2][getNumBranches()];
            for(int i = 0; i < getNumBranches(); i++) {
                newObject.tauBranches[0][i] = (TauBranch)tauBranches[0][i].clone();
                newObject.tauBranches[1][i] = (TauBranch)tauBranches[1][i].clone();
            }
            return newObject;
        } catch(CloneNotSupportedException e) {
            // Can't happen, but...
            System.err.println("Caught CloneNotSupportedException: "
                    + e.getMessage());
            throw new InternalError(e.toString());
        }
    }

    public String toString() {
        if(DEBUG) System.out.println("Starting toString() in TauModel");
        String desc = "Delta tau for each slowness sample and layer.\n";
        for(int j = 0; j < rayParams.length; j++) {
            for(int i = 0; i < tauBranches[0].length; i++) {
                desc += " i " + i + " j " + j + " rayParam " + rayParams[j]
                        + " tau " + tauBranches[0][i].tau[j] + " time "
                        + tauBranches[0][i].time[j] + " dist "
                        + tauBranches[0][i].getDist(j) + " degrees "
                        + (tauBranches[0][i].getDist(j) * 180 / Math.PI) + "\n";
            }
            desc += "\n";
        }
        return desc;
    }

    public static void main(String[] args) {
        VelocityModel vMod = new VelocityModel();
        SphericalSModel sMod = new SphericalSModel();
        TauModel tMod = new TauModel();
        int branch, rayNum;
        String modelFilename;
        if(args.length == 1) {
            modelFilename = args[0];
        } else {
            modelFilename = "iasp91.tvel";
        }
        boolean DEBUG = false;
        try {
            vMod.setFileType("tvel");
            vMod.readVelocityFile(modelFilename);
            System.out.println("Done reading.");
            sMod.createSample(vMod);
            tMod.calcTauIncFrom(sMod);
            StreamTokenizer tokenIn = new StreamTokenizer(new InputStreamReader(System.in));
            tokenIn.parseNumbers();
            System.out.println("Enter branch rayNum");
            tokenIn.nextToken();
            while(tokenIn.ttype == StreamTokenizer.TT_NUMBER) {
                branch = (int)tokenIn.nval;
                tokenIn.nextToken();
                rayNum = (int)tokenIn.nval;
                System.out.println("ray parameter=" + tMod.rayParams[rayNum]
                        + " distance="
                        + tMod.tauBranches[0][branch].getDist(rayNum) + " time="
                        + tMod.tauBranches[0][branch].time[rayNum] + " tau="
                        + tMod.tauBranches[0][branch].tau[rayNum]);
                System.out.println("Enter branch rayNum");
                tokenIn.nextToken();
            }
        } catch(IOException e) {
            System.out.println("Tried to read!\n Caught IOException "
                    + e.getMessage());
        } catch(VelocityModelException e) {
            System.out.println("Tried to read!\n Caught VelocityModelException "
                    + e.getMessage());
        } catch(SlownessModelException e) {
            System.out.println("Caught SlownessModelException "
                    + e.getMessage());
            e.printStackTrace();
        } catch(TauModelException e) {
            System.out.println("Caught TauModelException " + e.getMessage());
            e.printStackTrace();
        } finally {
            System.out.println("Done!\n");
        }
    }
}