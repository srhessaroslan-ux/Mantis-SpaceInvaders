package screen;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import engine.Achievement;
import engine.AchievementManager;
import engine.Cooldown;
import engine.Core;
import engine.DrawManager;

/**
 * Implements the achievements screen, split into pages.
 * Page 1 shows normal achievements, page 2 shows tier achievements.
 */
public class AchievementsScreen extends Screen {
	/**
	 * Colour of the row the player has selected.
	 */
	private static final Color SELECTED = Color.GREEN;
	/**
	 * Colour of rows that are not selected.
	 */
	private static final Color UNSELECTED = Color.WHITE;
	/**
	 * Colour of secondary text and locked items.
	 */
	private static final Color MUTED = Color.GRAY;
	/**
	 * Colour of an unlocked achievement's icon.
	 */
	private static final Color UNLOCKED = Color.YELLOW;
	/**
	 * Colour of a locked achievement's icon.
	 */
	private static final Color LOCKED = Color.DARK_GRAY;
	/** Gap between the title baseline and the page label. */
	private static final int TITLE_TO_PAGE_GAP = 70;
	/** Gap between the page label and the first achievement row. */
	private static final int PAGE_TO_ROWS_GAP = 30;
	/** Maximum vertical distance between achievement rows. */
	private static final int MAX_ROW_SPACING = 75;
	/** Minimum vertical distance between achievement rows on short screens. */
	private static final int MIN_ROW_SPACING = 36;
	/** Space reserved at the bottom for key hints and breathing room. */
	private static final int BOTTOM_RESERVED = 80;
	/** Lowest text line drawn inside an achievement row. */
	private static final int ROW_TEXT_BOTTOM = 24;
	/** Width reserved for the achievement list, used to center it. */
	private static final int CONTENT_WIDTH = 420;
	/**
	 * Horizontal position of the trophy icon before centering.
	 */
	private static final int TROPHY_X = 30;
	/**
	 * Horizontal position of the name and description.
	 */
	private static final int TEXT_X = 70;
	/**
	 * Horizontal position of the status area.
	 */
	private static final int STATUS_X = 320;
	/**
	 * Time between selection moves, in milliseconds.
	 */
	private static final int SELECTION_INTERVAL = 200;
	/**
	 * Name of each page, in page order.
	 */
	private static final String[] PAGE_NAMES = {"Normal", "Tier"};

	/**
	 * Scroll function
	 */
	private int scrollOffset;
	/**
	 * Achievements on each page, in page order.
	 */
	private List<List<Achievement>> pages;
	/**
	 * Index of the page currently shown.
	 */
	private int currentPage;
	/**
	 * Enemies defeated across all games.
	 */
	private int totalKills;
	/**
	 * Index of the selected row on the current page.
	 */
	private int selected;
	/**
	 * Stops the selection moving on every frame.
	 */
	private Cooldown selectionCooldown;

	/**
	 * Constructor, establishes the properties of the screen.
	 *
	 * @param width  Screen width.
	 * @param height Screen height.
	 * @param fps    Frames per second.
	 */
	public AchievementsScreen(final int width, final int height,
							  final int fps) {
		super(width, height, fps);
		// Return to the main menu when this screen closes.
		this.returnCode = 1;
		AchievementManager manager = Core.getAchievementManager();
		this.pages = new ArrayList<List<Achievement>>();
		this.pages.add(manager.getNormalAchievements());
		this.pages.add(manager.getTierAchievements());
		this.totalKills = manager.getTotalEnemiesKilled();
		this.currentPage = 0;
		this.selected = 0;
		this.scrollOffset = 0;
		this.selectionCooldown = Core.getCooldown(SELECTION_INTERVAL);
		this.selectionCooldown.reset();
	}

	/**
	 * Starts the screen.
	 *
	 * @return Next screen code.
	 */
	@Override
	public final int run() {
		super.run();
		return this.returnCode;
	}

	/**
	 * Draws the screen and checks for keyboard input.
	 */
	@Override
	protected final void update() {
		super.update();
		draw();
		if (this.inputDelay.checkFinished()
				&& this.selectionCooldown.checkFinished()) {
			if (this.inputManager.isKeyDown(KeyEvent.VK_UP)
					|| this.inputManager.isKeyDown(KeyEvent.VK_W)) {
				if (this.selected > 0) {
					this.selected--;
					this.selectionCooldown.reset();
				}
			}
			if (this.inputManager.isKeyDown(KeyEvent.VK_DOWN)
					|| this.inputManager.isKeyDown(KeyEvent.VK_S)) {
				if (this.selected
						< this.pages.get(this.currentPage).size() - 1) {
					this.selected++;
					this.selectionCooldown.reset();
				}
			}
			if (this.inputManager.isKeyDown(KeyEvent.VK_LEFT)
					|| this.inputManager.isKeyDown(KeyEvent.VK_A)) {
				if (this.currentPage > 0) {
					this.currentPage--;
					this.selected = 0;
					this.scrollOffset = 0;
					this.selectionCooldown.reset();
				}
			}
			if (this.inputManager.isKeyDown(KeyEvent.VK_RIGHT)
					|| this.inputManager.isKeyDown(KeyEvent.VK_D)) {
				if (this.currentPage < this.pages.size() - 1) {
					this.currentPage++;
					this.selected = 0;
					this.scrollOffset = 0;
					this.selectionCooldown.reset();
				}
			}
			int visibleRows = AchievementManager.ACHIEVEMENTS_PER_PAGE;

			if (this.selected < this.scrollOffset) {
				this.scrollOffset = this.selected;
			} else if (this.selected >= this.scrollOffset + visibleRows) {
				this.scrollOffset = this.selected - visibleRows + 1;
			}
		}
		if (this.inputManager.isKeyDown(KeyEvent.VK_ESCAPE)
				&& this.inputDelay.checkFinished()) {
			this.isRunning = false;
		}
	}

	/**
	 * @return Number of rows shown on the current page, at most
	 * {@link AchievementManager#ACHIEVEMENTS_PER_PAGE}.
	 */
	private int getRowCount() {
		int remaining = this.pages.get(this.currentPage).size()
				- this.scrollOffset;

		return Math.min(
				remaining,
				AchievementManager.ACHIEVEMENTS_PER_PAGE);
	}

	/**
	 * @return Horizontal offset that centers the achievement content.
	 */
	private int getContentOffsetX() {
		return Math.max(0, (this.width - CONTENT_WIDTH) / 2);
	}

	/**
	 * @return Baseline of the page label, positioned below the screen title.
	 */
	private int getPageLabelY() {
		return this.height / 8 + TITLE_TO_PAGE_GAP;
	}

	/**
	 * @return Vertical position of the first achievement row.
	 */
	private int getFirstRowY() {
		return getPageLabelY() + PAGE_TO_ROWS_GAP;
	}

	/**
	 * @return Row spacing, reduced when necessary to keep all visible rows
	 *         above the key hints on shorter screens.
	 */
	private int getRowSpacing() {
		int rowSlots = Math.max(1,
				AchievementManager.ACHIEVEMENTS_PER_PAGE - 1);
		int available = this.height - BOTTOM_RESERVED
				- getFirstRowY() - ROW_TEXT_BOTTOM;
		int spacing = available / rowSlots;
		return Math.max(MIN_ROW_SPACING,
				Math.min(MAX_ROW_SPACING, spacing));
	}

	/**
	 * Draws the title, the page label, the current page's rows, and the
	 * key hints.
	 */
	private void draw() {
		this.drawManager.initDrawing(this);
		this.drawManager.drawScreenTitle(
				this, MenuItem.ACHIEVEMENTS.getTitle());

		String pageLabel = "< " + PAGE_NAMES[this.currentPage] + "  "
				+ (this.currentPage + 1) + "/" + this.pages.size() + " >";
		this.drawManager.drawMenuRow(
				this, pageLabel, getPageLabelY(), false);

		List<Achievement> page = this.pages.get(this.currentPage);
		int firstRowY = getFirstRowY();
		int rowSpacing = getRowSpacing();
		int rowCount = getRowCount();
		if (page.isEmpty()) {
			this.drawManager.drawRegularString("No achievements yet.",
					TEXT_X + getContentOffsetX(),
					firstRowY + ROW_TEXT_BOTTOM, MUTED);
		} else {
			for (int i = 0; i < rowCount; i++) {
				int achievementIndex = this.scrollOffset + i;

				drawAchievement(
						page.get(achievementIndex),
						firstRowY + i * rowSpacing,
						achievementIndex == this.selected);
			}
		}
		if (this.scrollOffset + rowCount < page.size()) {
			int lastRowY = firstRowY + (rowCount - 1) * rowSpacing;
			int moreY = Math.min(lastRowY + ROW_TEXT_BOTTOM + 18,
					this.height - BOTTOM_RESERVED / 2);
			this.drawManager.drawRegularString(
					" ------  MORE ACHIEVEMENT BELOW  ------",
					TEXT_X + getContentOffsetX(),
					moreY,
					MUTED);
		}


		this.drawManager.drawKeyHints(this,
				"left right page, up down move, esc back");
		this.drawManager.completeDrawing(this);
	}

	/**
	 * Draws a single achievement row.
	 *
	 * @param achievement Achievement to draw.
	 * @param positionY   Vertical position of the row's top edge.
	 * @param isSelected  Whether this row is currently highlighted.
	 */
	private void drawAchievement(final Achievement achievement,
								 final int positionY, final boolean isSelected) {

		Color trophyColor = achievement.isUnlocked()
				? achievement.getIconColor() : LOCKED;

		if (achievement.getSpriteType() == DrawManager.SpriteType.FleetMaster) {
			trophyColor = achievement.isUnlocked()
					? Color.WHITE : Color.GRAY;
		}

		Color nameColor = isSelected ? SELECTED : UNSELECTED;
		int horizontalOffset = getContentOffsetX();

		DrawManager.SpriteType icon = achievement.getSpriteType();

		if (icon == null)
			icon = DrawManager.SpriteType.FirstFlight;

		AchievementManager manager = Core.getAchievementManager();

		if (icon == DrawManager.SpriteType.FleetMaster) {
			this.drawManager.drawSmallSprite(
					icon, TROPHY_X + horizontalOffset, positionY, trophyColor);
		} else {
			this.drawManager.drawSprite(
					icon, TROPHY_X + horizontalOffset, positionY, trophyColor);
		}

		this.drawManager.drawRegularString(
				achievement.getName(),
				TEXT_X + horizontalOffset, positionY + 8, nameColor);

		this.drawManager.drawRegularString(
				manager.getRequirementText(achievement),
				TEXT_X + horizontalOffset, positionY + 24, MUTED);

		this.drawManager.drawRegularString(
				manager.getProgressText(achievement),
				STATUS_X + horizontalOffset, positionY + 8,
				achievement.isUnlocked() ? SELECTED : MUTED);
	}
}