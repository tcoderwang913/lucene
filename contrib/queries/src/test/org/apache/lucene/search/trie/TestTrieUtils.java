package org.apache.lucene.search.trie;

/**
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.util.OpenBitSet;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

public class TestTrieUtils extends LuceneTestCase {

  public void testLongConversionAndOrdering() throws Exception {
    // generate a series of encoded longs, each numerical one bigger than the one before
    String last=null;
    for (long l=-100000L; l<100000L; l++) {
      String act=TrieUtils.longToPrefixCoded(l);
      if (last!=null) {
        // test if smaller
        assertTrue("actual bigger than last", last.compareTo(act) < 0 );
      }
      // test is back and forward conversion works
      assertEquals("forward and back conversion should generate same long", l, TrieUtils.prefixCodedToLong(act));
      // next step
      last=act;
    }
  }

  public void testIntConversionAndOrdering() throws Exception {
    // generate a series of encoded ints, each numerical one bigger than the one before
    String last=null;
    for (int i=-100000; i<100000; i++) {
      String act=TrieUtils.intToPrefixCoded(i);
      if (last!=null) {
        // test if smaller
        assertTrue("actual bigger than last", last.compareTo(act) < 0 );
      }
      // test is back and forward conversion works
      assertEquals("forward and back conversion should generate same int", i, TrieUtils.prefixCodedToInt(act));
      // next step
      last=act;
    }
  }

  public void testLongSpecialValues() throws Exception {
    long[] vals=new long[]{
      Long.MIN_VALUE, Long.MIN_VALUE+1, Long.MIN_VALUE+2, -5003400000000L,
      -4000L, -3000L, -2000L, -1000L, -1L, 0L, 1L, 10L, 300L, 50006789999999999L, Long.MAX_VALUE-2, Long.MAX_VALUE-1, Long.MAX_VALUE
    };
    String[] prefixVals=new String[vals.length];
    
    for (int i=0; i<vals.length; i++) {
      prefixVals[i]=TrieUtils.longToPrefixCoded(vals[i]);
      
      // check forward and back conversion
      assertEquals( "forward and back conversion should generate same long", vals[i], TrieUtils.prefixCodedToLong(prefixVals[i]) );

      // test if decoding values as int fails correctly
      try {
        TrieUtils.prefixCodedToInt(prefixVals[i]);
        fail("decoding a prefix coded long value as int should fail");
      } catch (NumberFormatException e) {
        // worked
      }
    }
    
    // check sort order (prefixVals should be ascending)
    for (int i=1; i<prefixVals.length; i++) {
      assertTrue( "check sort order", prefixVals[i-1].compareTo( prefixVals[i] ) < 0 );
    }
        
    // check the prefix encoding, lower precision should have the difference to original value equal to the lower removed bits
    for (int i=0; i<vals.length; i++) {
      for (int j=0; j<64; j++) {
        long prefixVal=TrieUtils.prefixCodedToLong(TrieUtils.longToPrefixCoded(vals[i], j));
        long mask=(1L << j) - 1L;
        assertEquals( "difference between prefix val and original value for "+vals[i]+" with shift="+j, vals[i] & mask, vals[i]-prefixVal );
      }
    }
  }

  public void testIntSpecialValues() throws Exception {
    int[] vals=new int[]{
      Integer.MIN_VALUE, Integer.MIN_VALUE+1, Integer.MIN_VALUE+2, -64765767,
      -4000, -3000, -2000, -1000, -1, 0, 1, 10, 300, 765878989, Integer.MAX_VALUE-2, Integer.MAX_VALUE-1, Integer.MAX_VALUE
    };
    String[] prefixVals=new String[vals.length];
    
    for (int i=0; i<vals.length; i++) {
      prefixVals[i]=TrieUtils.intToPrefixCoded(vals[i]);
      
      // check forward and back conversion
      assertEquals( "forward and back conversion should generate same int", vals[i], TrieUtils.prefixCodedToInt(prefixVals[i]) );
      
      // test if decoding values as long fails correctly
      try {
        TrieUtils.prefixCodedToLong(prefixVals[i]);
        fail("decoding a prefix coded int value as long should fail");
      } catch (NumberFormatException e) {
        // worked
      }
    }
    
    // check sort order (prefixVals should be ascending)
    for (int i=1; i<prefixVals.length; i++) {
      assertTrue( "check sort order", prefixVals[i-1].compareTo( prefixVals[i] ) < 0 );
    }
    
    // check the prefix encoding, lower precision should have the difference to original value equal to the lower removed bits
    for (int i=0; i<vals.length; i++) {
      for (int j=0; j<32; j++) {
        int prefixVal=TrieUtils.prefixCodedToInt(TrieUtils.intToPrefixCoded(vals[i], j));
        int mask=(1 << j) - 1;
        assertEquals( "difference between prefix val and original value for "+vals[i]+" with shift="+j, vals[i] & mask, vals[i]-prefixVal );
      }
    }
  }

  public void testDoubles() throws Exception {
    double[] vals=new double[]{
      Double.NEGATIVE_INFINITY, -2.3E25, -1.0E15, -1.0, -1.0E-1, -1.0E-2, -0.0, 
      +0.0, 1.0E-2, 1.0E-1, 1.0, 1.0E15, 2.3E25, Double.POSITIVE_INFINITY
    };
    long[] longVals=new long[vals.length];
    
    // check forward and back conversion
    for (int i=0; i<vals.length; i++) {
      longVals[i]=TrieUtils.doubleToSortableLong(vals[i]);
      assertTrue( "forward and back conversion should generate same double", Double.compare(vals[i], TrieUtils.sortableLongToDouble(longVals[i]))==0 );
    }
    
    // check sort order (prefixVals should be ascending)
    for (int i=1; i<longVals.length; i++) {
      assertTrue( "check sort order", longVals[i-1] < longVals[i] );
    }
  }

  public void testFloats() throws Exception {
    float[] vals=new float[]{
      Float.NEGATIVE_INFINITY, -2.3E25f, -1.0E15f, -1.0f, -1.0E-1f, -1.0E-2f, -0.0f, 
      +0.0f, 1.0E-2f, 1.0E-1f, 1.0f, 1.0E15f, 2.3E25f, Float.POSITIVE_INFINITY
    };
    int[] intVals=new int[vals.length];
    
    // check forward and back conversion
    for (int i=0; i<vals.length; i++) {
      intVals[i]=TrieUtils.floatToSortableInt(vals[i]);
      assertTrue( "forward and back conversion should generate same double", Float.compare(vals[i], TrieUtils.sortableIntToFloat(intVals[i]))==0 );
    }
    
    // check sort order (prefixVals should be ascending)
    for (int i=1; i<intVals.length; i++) {
      assertTrue( "check sort order", intVals[i-1] < intVals[i] );
    }
  }
  
  // INFO: Tests for trieCodeLong()/trieCodeInt() not needed because implicitely tested by range filter tests
  
  /** Note: The neededBounds iterator must be unsigned (easier understanding what's happening) */
  protected void assertLongRangeSplit(final long lower, final long upper, int precisionStep,
    final boolean useBitSet, final Iterator neededBounds
  ) throws Exception {
    final OpenBitSet bits=useBitSet ? new OpenBitSet(upper-lower+1) : null;
    
    TrieUtils.splitLongRange(new TrieUtils.LongRangeBuilder() {
      //@Override
      public void addRange(long min, long max, int shift) {
        assertTrue("min, max should be inside bounds", min>=lower && min<=upper && max>=lower && max<=upper);
        if (useBitSet) for (long l=min; l<=max; l++) {
          assertFalse("ranges should not overlap", bits.getAndSet(l-lower) );
        }
        // make unsigned longs for easier display and understanding
        min ^= 0x8000000000000000L;
        max ^= 0x8000000000000000L;
        //System.out.println("new Long(0x"+Long.toHexString(min>>>shift)+"L),new Long(0x"+Long.toHexString(max>>>shift)+"L),");
        assertEquals( "inner min bound", ((Long)neededBounds.next()).longValue(), min>>>shift);
        assertEquals( "inner max bound", ((Long)neededBounds.next()).longValue(), max>>>shift);
      }
    }, precisionStep, lower, upper);
    
    if (useBitSet) {
      // after flipping all bits in the range, the cardinality should be zero
      bits.flip(0,upper-lower+1);
      assertTrue("The sub-range concenated should match the whole range", bits.isEmpty());
    }
  }
  
  public void testSplitLongRange() throws Exception {
    // a hard-coded "standard" range
    assertLongRangeSplit(-5000L, 9500L, 4, true, Arrays.asList(new Long[]{
      new Long(0x7fffffffffffec78L),new Long(0x7fffffffffffec7fL),
      new Long(0x8000000000002510L),new Long(0x800000000000251cL),
      new Long(0x7fffffffffffec8L), new Long(0x7fffffffffffecfL),
      new Long(0x800000000000250L), new Long(0x800000000000250L),
      new Long(0x7fffffffffffedL),  new Long(0x7fffffffffffefL),
      new Long(0x80000000000020L),  new Long(0x80000000000024L),
      new Long(0x7ffffffffffffL),   new Long(0x8000000000001L)
    }).iterator());
    
    // the same with no range splitting
    assertLongRangeSplit(-5000L, 9500L, 64, true, Arrays.asList(new Long[]{
      new Long(0x7fffffffffffec78L),new Long(0x800000000000251cL)
    }).iterator());
    
    // this tests optimized range splitting, if one of the inner bounds
    // is also the bound of the next lower precision, it should be used completely
    assertLongRangeSplit(0L, 1024L+63L, 4, true, Arrays.asList(new Long[]{
      new Long(0x800000000000040L), new Long(0x800000000000043L),
      new Long(0x80000000000000L),  new Long(0x80000000000003L)
    }).iterator());
    
    // the full long range should only consist of a lowest precision range; no bitset testing here, as too much memory needed :-)
    assertLongRangeSplit(Long.MIN_VALUE, Long.MAX_VALUE, 8, false, Arrays.asList(new Long[]{
      new Long(0x00L),new Long(0xffL)
    }).iterator());

    // the same with precisionStep=4
    assertLongRangeSplit(Long.MIN_VALUE, Long.MAX_VALUE, 4, false, Arrays.asList(new Long[]{
      new Long(0x0L),new Long(0xfL)
    }).iterator());

    // the same with precisionStep=2
    assertLongRangeSplit(Long.MIN_VALUE, Long.MAX_VALUE, 2, false, Arrays.asList(new Long[]{
      new Long(0x0L),new Long(0x3L)
    }).iterator());

    // the same with precisionStep=1
    assertLongRangeSplit(Long.MIN_VALUE, Long.MAX_VALUE, 1, false, Arrays.asList(new Long[]{
      new Long(0x0L),new Long(0x1L)
    }).iterator());

    // a inverse range should produce no sub-ranges
    assertLongRangeSplit(9500L, -5000L, 4, false, Collections.EMPTY_LIST.iterator());    

    // a 0-length range should reproduce the range itsself
    assertLongRangeSplit(9500L, 9500L, 4, false, Arrays.asList(new Long[]{
      new Long(0x800000000000251cL),new Long(0x800000000000251cL)
    }).iterator());
  }

  /** Note: The neededBounds iterator must be unsigned (easier understanding what's happening) */
  protected void assertIntRangeSplit(final int lower, final int upper, int precisionStep,
    final boolean useBitSet, final Iterator neededBounds
  ) throws Exception {
    final OpenBitSet bits=useBitSet ? new OpenBitSet(upper-lower+1) : null;
    
    TrieUtils.splitIntRange(new TrieUtils.IntRangeBuilder() {
      //@Override
      public void addRange(int min, int max, int shift) {
        assertTrue("min, max should be inside bounds", min>=lower && min<=upper && max>=lower && max<=upper);
        if (useBitSet) for (int i=min; i<=max; i++) {
          assertFalse("ranges should not overlap", bits.getAndSet(i-lower) );
        }
        // make unsigned ints for easier display and understanding
        min ^= 0x80000000;
        max ^= 0x80000000;
        //System.out.println("new Integer(0x"+Integer.toHexString(min>>>shift)+"),new Integer(0x"+Integer.toHexString(max>>>shift)+"),");
        assertEquals( "inner min bound", ((Integer)neededBounds.next()).intValue(), min>>>shift);
        assertEquals( "inner max bound", ((Integer)neededBounds.next()).intValue(), max>>>shift);
      }
    }, precisionStep, lower, upper);
    
    if (useBitSet) {
      // after flipping all bits in the range, the cardinality should be zero
      bits.flip(0,upper-lower+1);
      assertTrue("The sub-range concenated should match the whole range", bits.isEmpty());
    }
  }
  
  public void testSplitIntRange() throws Exception {
    // a hard-coded "standard" range
    assertIntRangeSplit(-5000, 9500, 4, true, Arrays.asList(new Integer[]{
      new Integer(0x7fffec78),new Integer(0x7fffec7f),
      new Integer(0x80002510),new Integer(0x8000251c),
      new Integer(0x7fffec8), new Integer(0x7fffecf),
      new Integer(0x8000250), new Integer(0x8000250),
      new Integer(0x7fffed),  new Integer(0x7fffef),
      new Integer(0x800020),  new Integer(0x800024),
      new Integer(0x7ffff),   new Integer(0x80001)
    }).iterator());
    
    // the same with no range splitting
    assertIntRangeSplit(-5000, 9500, 32, true, Arrays.asList(new Integer[]{
      new Integer(0x7fffec78),new Integer(0x8000251c)
    }).iterator());
    
    // this tests optimized range splitting, if one of the inner bounds
    // is also the bound of the next lower precision, it should be used completely
    assertIntRangeSplit(0, 1024+63, 4, true, Arrays.asList(new Integer[]{
      new Integer(0x8000040), new Integer(0x8000043),
      new Integer(0x800000),  new Integer(0x800003)
    }).iterator());
    
    // the full int range should only consist of a lowest precision range; no bitset testing here, as too much memory needed :-)
    assertIntRangeSplit(Integer.MIN_VALUE, Integer.MAX_VALUE, 8, false, Arrays.asList(new Integer[]{
      new Integer(0x00),new Integer(0xff)
    }).iterator());

    // the same with precisionStep=4
    assertIntRangeSplit(Integer.MIN_VALUE, Integer.MAX_VALUE, 4, false, Arrays.asList(new Integer[]{
      new Integer(0x0),new Integer(0xf)
    }).iterator());

    // the same with precisionStep=2
    assertIntRangeSplit(Integer.MIN_VALUE, Integer.MAX_VALUE, 2, false, Arrays.asList(new Integer[]{
      new Integer(0x0),new Integer(0x3)
    }).iterator());

    // the same with precisionStep=1
    assertIntRangeSplit(Integer.MIN_VALUE, Integer.MAX_VALUE, 1, false, Arrays.asList(new Integer[]{
      new Integer(0x0),new Integer(0x1)
    }).iterator());

    // a inverse range should produce no sub-ranges
    assertIntRangeSplit(9500, -5000, 4, false, Collections.EMPTY_LIST.iterator());    

    // a 0-length range should reproduce the range itsself
    assertIntRangeSplit(9500, 9500, 4, false, Arrays.asList(new Integer[]{
      new Integer(0x8000251c),new Integer(0x8000251c)
    }).iterator());
  }

}
