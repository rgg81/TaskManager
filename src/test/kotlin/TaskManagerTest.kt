import io.kotest.data.forAll
import io.kotest.data.headers
import io.kotest.data.row
import io.kotest.data.table
import io.kotest.matchers.collections.*
import io.kotest.matchers.shouldBe
import manager.*
import model.Priority
import model.SOProcess
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test


class TaskManagerTest {

    @Nested
    inner class AddDefault{
        @Test
        fun `capacity test`() {
            val taskManager = TaskManager(AddProcessDefault(3))
            table(
                headers("process", "length"),
                row(Priority.LOW, 1),
                row(Priority.LOW, 2),
                row(Priority.LOW, 3),
                row(Priority.LOW, 3),
                row(Priority.LOW, 3),
                row(Priority.LOW, 3),

            ).forAll { process, length ->
                val proc = SOProcess(process)
                taskManager.add(proc)
                taskManager.list().size shouldBe length
            }

        }

        @Test
        fun `order test`() {
            val taskManager = TaskManager(AddProcessDefault(1))
            table(
                headers("processId", "expectedList"),
                row("1", ArrayList(listOf(SOProcess(Priority.HIGH, "1")))),
                row("2", ArrayList(listOf(SOProcess(Priority.HIGH, "1")))),
                row("3", ArrayList(listOf(SOProcess(Priority.HIGH, "1")))),
                row("4", ArrayList(listOf(SOProcess(Priority.HIGH, "1")))),

                ).forAll { processId, expectedList ->
                val proc = SOProcess(Priority.HIGH, processId)
                taskManager.add(proc)
                taskManager.list() shouldBe expectedList
            }

        }
    }

    @Nested
    inner class AddFifo{
        @Test
        fun `capacity test`() {
            val taskManager = TaskManager(AddProcessFiFo(3))
            table(
                headers("process", "length"),
                row(Priority.LOW, 1),
                row(Priority.LOW, 2),
                row(Priority.LOW, 3),
                row(Priority.LOW, 3),
                row(Priority.LOW, 3),
                row(Priority.LOW, 3),

                ).forAll { process, length ->
                val proc = SOProcess(process)
                taskManager.add(proc)
                taskManager.list().size shouldBe length
            }

        }

        @Test
        fun `order test`() {
            val taskManager = TaskManager(AddProcessFiFo(2))
            table(
                headers("processId", "expectedList"),
                row("1", ArrayList(listOf(SOProcess(Priority.HIGH, "1")))),
                row("2", ArrayList(listOf(SOProcess(Priority.HIGH, "1"), SOProcess(Priority.HIGH, "2")))),
                row("3", ArrayList(listOf(SOProcess(Priority.HIGH, "2"), SOProcess(Priority.HIGH, "3")))),
                row("4", ArrayList(listOf(SOProcess(Priority.HIGH, "3"), SOProcess(Priority.HIGH, "4")))),

                ).forAll { processId, expectedList ->
                val proc = SOProcess(Priority.HIGH, processId)
                taskManager.add(proc)
                taskManager.list() shouldBe expectedList
            }

        }
    }

    @Nested
    inner class AddPriority{
        @Test
        fun `capacity test`() {
            val taskManager = TaskManager(AddProcessPriority(2))
            table(
                headers("process", "length"),
                row(Priority.LOW, 1),
                row(Priority.LOW, 2),
                row(Priority.LOW, 2),
                row(Priority.LOW, 2),
                row(Priority.LOW, 2),
                row(Priority.LOW, 2),

                ).forAll { process, length ->
                val proc = SOProcess(process)
                taskManager.add(proc)
                taskManager.list().size shouldBe length
            }

        }

        @Test
        fun `order test`() {
            val taskManager = TaskManager(AddProcessPriority(2))
            table(
                headers("processId", "priority","expectedList"),

                row("2", Priority.LOW, ArrayList(listOf(SOProcess(Priority.LOW, "2")))),
                row("3", Priority.MEDIUM,
                    ArrayList(
                        listOf(
                            SOProcess(Priority.LOW, "2"),
                            SOProcess(Priority.MEDIUM, "3")
                        )
                    )
                ),
                row("1", Priority.MEDIUM,
                    ArrayList(
                        listOf(
                            SOProcess(Priority.MEDIUM, "3"),
                            SOProcess(Priority.MEDIUM, "1")
                        )
                    )
                ),
                row("4", Priority.LOW,
                    ArrayList(
                        listOf(
                            SOProcess(Priority.MEDIUM, "3"),
                            SOProcess(Priority.MEDIUM, "1")
                        )
                    )
                ),
                row("5", Priority.LOW,
                    ArrayList(
                        listOf(
                            SOProcess(Priority.MEDIUM, "3"),
                            SOProcess(Priority.MEDIUM, "1")
                        )
                    )
                ),
                row("6", Priority.MEDIUM,
                    ArrayList(
                        listOf(
                            SOProcess(Priority.MEDIUM, "3"),
                            SOProcess(Priority.MEDIUM, "1")
                        )
                    )
                ),
                row("7", Priority.HIGH,
                    ArrayList(
                        listOf(
                            SOProcess(Priority.MEDIUM, "1"),
                            SOProcess(Priority.HIGH, "7"),
                        )
                    )
                ),

                ).forAll { processId, priority, expectedList ->
                val proc = SOProcess(priority, processId)
                taskManager.add(proc)
                taskManager.list() shouldBe expectedList
            }

        }
    }

    @Nested
    inner class KillTests {
        private val taskManager = run {
            val aTask = TaskManager(AddProcessDefault(30))
            for (i in 1..10) aTask.add(SOProcess(Priority.LOW, i.toString()))
            for (i in 11..20) aTask.add(SOProcess(Priority.MEDIUM, i.toString()))
            for (i in 21..30) aTask.add(SOProcess(Priority.HIGH, i.toString()))
            aTask
        }

        @Test
        fun `kill a process`(){
            table(
                headers("processId", "length", "resultKill"),
                row("1", 29, true),
                row("1", 29, false),
                row("2", 28, true),
                row("2", 28, false),
                row("3", 27, true),
                row("3", 27, false),
                row("12", 27, false),
            ).forAll { processId, length, resultKill ->
                val resultKillFun = taskManager.kill(SOProcess(Priority.LOW, processId))
                resultKillFun shouldBe resultKill
                taskManager.list().size shouldBe length
            }
        }

        @Test
        fun `kill all process`(){
            taskManager.list() shouldHaveSize 30
            taskManager.killAll()
            taskManager.list() shouldHaveSize 0
        }

        @Test
        fun `kill per priority`(){
            taskManager.list() shouldHaveSize 30
            table(
                headers("priority","processId", "length"),
                row(Priority.LOW, "1", 20),
                row(Priority.MEDIUM, "13", 10),
                row(Priority.MEDIUM, "17", 10),
                row(Priority.HIGH, "27", 0),
            ).forAll {priority, processId, length ->
                taskManager.killGroup(priority)
                taskManager.list() shouldHaveSize length
                taskManager.list() shouldNotContain SOProcess(priority, processId)
            }
        }
    }
    
    @Nested
    inner class ListTest {

        private val taskManager = run {
            val aTask = TaskManager(AddProcessDefault(30))
            for (i in 'e'..'h') aTask.add(SOProcess(Priority.LOW, i.toString()))
            for (i in 'a'..'d') aTask.add(SOProcess(Priority.HIGH, i.toString()))
            for (i in 'i'..'m') aTask.add(SOProcess(Priority.MEDIUM, i.toString()))
            aTask
        }

        @Test
        fun `order by created date`(){
            val listCreatedDate = taskManager.list()
            listCreatedDate shouldStartWith SOProcess(Priority.LOW, "e")
            listCreatedDate shouldBeSortedWith { s1, s2 ->
                s1.createdDate.compareTo(s2.createdDate)
            }
        }

        @Test
        fun `order by priority`(){
            val listCreatedDate = taskManager.list(SortedBy.PRIORITY)
            listCreatedDate shouldBeSortedWith { s1, s2 ->
                s1.priority.compareTo(s2.priority)
            }
        }

        @Test
        fun `order by id`(){
            val listCreatedDate = taskManager.list(SortedBy.ID)
            listCreatedDate shouldBeSortedWith { s1, s2 ->
                s1.pid.compareTo(s2.pid)
            }
        }
        
    }
}