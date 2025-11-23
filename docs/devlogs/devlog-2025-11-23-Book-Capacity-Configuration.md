# DEVLOG ENTRY

## Date: 11-23-25

### Feature: Granular Book Capacity Configuration

#### What Changed
Previously, bookcases could only specify a "shelf capacity" (number of shelves), 
with no control over how many books each shelf could hold. This update introduces 
a `bookCapacity` field that lets users specify books-per-shelf when creating a 
bookcase.

#### Key Changes
1. **Data Model Updates**
   - Added `bookCapacity` to BookcaseEntity (total capacity for the bookcase)
   - Renamed `ShelfEntity.shelfCapacity` → `bookCapacity` (per-shelf capacity)
   - Updated BookcaseDTO to include bookCapacity

2. **Improved CLI User Experience**
   - Better prompts: "What should we call this new bookcase?" vs "Give this 
     bookcase a label"
   - Three-step input: label → shelf count → books per shelf
   - Added confirmation screen showing:
     * Bookcase label
     * Number of shelves
     * Books per shelf
     * Total storage calculation
   - Y/N confirmation before database commit

3. **Service Layer**
   - BookcaseService.createNewBookCase() now accepts (label, shelfCapacity, 
     bookCapacity)
   - Total bookcase capacity calculated as `shelfCapacity * bookCapacity`
   - Each shelf receives its bookCapacity from the parent bookcase

#### Why This Matters
This separates two distinct domain concepts that were previously conflated:
- **Shelf count**: The physical structure of the bookcase
- **Book capacity**: The storage capability per shelf

This allows modeling real-world scenarios like:
- Small reference bookcase: 3 shelves × 15 books = 45 total
- Large fiction bookcase: 8 shelves × 30 books = 240 total

#### Testing Notes
- Manually tested CLI flow with various inputs
- Confirmed shelves inherit correct bookCapacity
- Tested confirmation rejection (bookcase not created)

#### Open Questions
1. Should we validate that bookCapacity > 0?
3. Is the confirmation step too verbose for power users? (Could add a --skip-confirm flag)

#### Next Steps
- [ ] Add validation for bookCapacity (must be positive)
- [ ] Write unit tests for BookcaseService.createNewBookCase()
- [ ] Update REST API documentation