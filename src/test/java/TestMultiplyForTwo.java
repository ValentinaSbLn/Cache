import cache.Cache;

import static cache.CacheType.FILE;

/**
* Created by Valentina on 21.10.2016.
*/
public class TestMultiplyForTwo implements TestMultiplyFor {

   private final int two = 2;

   @Override
   @Cache(cacheType= FILE)
   public int multiply(int d) {
       return two * d;
   }

   @Override
   public int powOfTwo(int d) {
       int mult = 1;
       for (int i = 1; i <= d; i++) {
           mult = mult * two;
       }
       return mult;
   }

   @Override
   @Cache
   public int multiplyCount(int d) {
       return d * d;
   }

}
