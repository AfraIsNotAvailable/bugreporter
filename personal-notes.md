- ma ocup de setul 3 de features

# SecurityContext on verifying if authorId is the same as requestingUserId
about the `requestingUserId` in `CommentController`:
there are 3 options to implement that:
1. using `java.security.Principal` 
   Instead of passing `@RequestParam Long requestingUserId` we can use the `Principal` object to get the authenticated
   user's information, something like this:
   ```java
   Long requestingUserId = Long.parseLong(principal.getName());
   ```
2. using `@AuthenticationPrincipal` annotation
   We can also use the `@AuthenticationPrincipal` annotation to directly inject the authenticated user's details into the controller method, like this:
   ```java
   @ AuthenticationPrincipal Long authorId
   // rest of the code
   ```
   i dont get this either so whatever
3. using `Authentication` object
   We can also access the `Authentication` object to get the authenticated user's details, like this:
   ```java
   Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
   Long requestingUserId = Long.parseLong(authentication.getName());
   ```

