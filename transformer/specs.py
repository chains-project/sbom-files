from enum import Enum

class SpecType(Enum):
    """Enum for spec type."""
    CYCLONEDX = 'cyclonedx'
    SPDX = 'spdx'
    JBOM = 'jbom'

    
    def __str__(self):
        return self.value