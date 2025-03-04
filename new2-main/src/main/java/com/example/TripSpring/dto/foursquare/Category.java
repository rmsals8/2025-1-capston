package com.example.TripSpring.dto.foursquare;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Category {
   private String id;
   private String name;
   private Icon icon;

   @Data
   @NoArgsConstructor
   @AllArgsConstructor
   public static class Icon {
       private String prefix;
       private String suffix;
   }
}