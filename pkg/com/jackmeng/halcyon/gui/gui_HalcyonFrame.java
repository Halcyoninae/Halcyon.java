package com.jackmeng.halcyon.gui;

import com.jackmeng.halcyon.use_HalcyonProperties;
import com.jackmeng.sys.pstream;
import com.jackmeng.sys.use_Program;
import com.jackmeng.sys.use_Task;
import com.jackmeng.util.use_Color;
import com.jackmeng.util.use_Image;
import com.jackmeng.util.use_ResourceFetcher;
import com.jackmeng.util.use_Struct.struct_Trio;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class gui_HalcyonFrame implements Runnable {

  public static class TitledFrame implements Runnable {
    public static class ComponentResizer extends MouseAdapter {
      private static final Dimension MINIMUM_SIZE = new Dimension(10, 10);
      private static final Dimension MAXIMUM_SIZE = new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);

      private static final Map<Integer, Integer> cursors = new HashMap<>();
      static {
        cursors.put(1, Cursor.N_RESIZE_CURSOR);
        cursors.put(2, Cursor.W_RESIZE_CURSOR);
        cursors.put(4, Cursor.S_RESIZE_CURSOR);
        cursors.put(8, Cursor.E_RESIZE_CURSOR);
        cursors.put(3, Cursor.NW_RESIZE_CURSOR);
        cursors.put(9, Cursor.NE_RESIZE_CURSOR);
        cursors.put(6, Cursor.SW_RESIZE_CURSOR);
        cursors.put(12, Cursor.SE_RESIZE_CURSOR);
      }

      private Insets dragInsets;
      private Dimension snapSize;

      private int direction;
      protected static final int NORTH = 1;
      protected static final int WEST = 2;
      protected static final int SOUTH = 4;
      protected static final int EAST = 8;

      private Cursor sourceCursor;
      private boolean resizing;
      private Rectangle bounds;
      private Point pressed;
      private boolean autoscrolls;

      private Dimension minimumSize = MINIMUM_SIZE;
      private Dimension maximumSize = MAXIMUM_SIZE;

      public ComponentResizer() {
        this(new Insets(5, 5, 5, 5), new Dimension(1, 1));
      }

      public ComponentResizer(Insets dragInsets, Dimension snapSize, Component... components) {
        setDragInsets(dragInsets);
        setSnapSize(snapSize);
        registerComponent(components);
      }

      public void setDragInsets(Insets dragInsets) {
        validateMinimumAndInsets(minimumSize, dragInsets);

        this.dragInsets = dragInsets;
      }

      public void setMaximumSize(Dimension maximumSize) {
        this.maximumSize = maximumSize;
      }

      public void setMinimumSize(Dimension minimumSize) {
        validateMinimumAndInsets(minimumSize, dragInsets);

        this.minimumSize = minimumSize;
      }

      public void deregisterComponent(Component... components) {
        for (Component component : components) {
          component.removeMouseListener(this);
          component.removeMouseMotionListener(this);
        }
      }

      public void registerComponent(Component... components) {
        for (Component component : components) {
          component.addMouseListener(this);
          component.addMouseMotionListener(this);
        }
      }

      public void setSnapSize(Dimension snapSize) {
        this.snapSize = snapSize;
      }

      private void validateMinimumAndInsets(Dimension minimum, Insets drag) {
        int minimumWidth = drag.left + drag.right;
        int minimumHeight = drag.top + drag.bottom;

        if (minimum.width < minimumWidth
            || minimum.height < minimumHeight) {
          String message = "Minimum size cannot be less than drag insets";
          throw new IllegalArgumentException(message);
        }
      }

      @Override
      public void mouseMoved(MouseEvent e) {
        Component source = e.getComponent();
        Point location = e.getPoint();
        direction = 0;

        if (location.x < dragInsets.left)
          direction += WEST;

        if (location.x > source.getWidth() - dragInsets.right - 1)
          direction += EAST;

        if (location.y < dragInsets.top)
          direction += NORTH;

        if (location.y > source.getHeight() - dragInsets.bottom - 1)
          direction += SOUTH;

        if (direction == 0) {
          source.setCursor(sourceCursor);
        } else {
          int cursorType = cursors.get(direction);
          Cursor cursor = Cursor.getPredefinedCursor(cursorType);
          source.setCursor(cursor);
        }
      }

      @Override
      public void mouseEntered(MouseEvent e) {
        if (!resizing) {
          Component source = e.getComponent();
          sourceCursor = source.getCursor();
        }
      }

      @Override
      public void mouseExited(MouseEvent e) {
        if (!resizing) {
          Component source = e.getComponent();
          source.setCursor(sourceCursor);
        }
      }

      @Override
      public void mousePressed(MouseEvent e) {
        if (direction == 0)
          return;
        resizing = true;
        Component source = e.getComponent();
        pressed = e.getPoint();
        SwingUtilities.convertPointToScreen(pressed, source);
        bounds = source.getBounds();
        if (source instanceof JComponent) {
          JComponent jc = (JComponent) source;
          autoscrolls = jc.getAutoscrolls();
          jc.setAutoscrolls(false);
        }
      }

      @Override
      public void mouseReleased(MouseEvent e) {
        resizing = false;

        Component source = e.getComponent();
        source.setCursor(sourceCursor);

        if (source instanceof JComponent) {
          ((JComponent) source).setAutoscrolls(autoscrolls);
        }
      }

      @Override
      public void mouseDragged(MouseEvent e) {
        if (!resizing)
          return;

        Component source = e.getComponent();
        Point dragged = e.getPoint();
        SwingUtilities.convertPointToScreen(dragged, source);

        changeBounds(source, direction, bounds, pressed, dragged);
      }

      protected void changeBounds(Component source, int direction, Rectangle bounds, Point pressed, Point current) {
        int x = bounds.x;
        int y = bounds.y;
        int width = bounds.width;
        int height = bounds.height;
        if (WEST == (direction & WEST)) {
          int drag = getDragDistance(pressed.x, current.x, snapSize.width);
          int maximum = Math.min(width + x, maximumSize.width);
          drag = getDragBounded(drag, snapSize.width, width, minimumSize.width, maximum);

          x -= drag;
          width += drag;
        }

        if (NORTH == (direction & NORTH)) {
          int drag = getDragDistance(pressed.y, current.y, snapSize.height);
          int maximum = Math.min(height + y, maximumSize.height);
          drag = getDragBounded(drag, snapSize.height, height, minimumSize.height, maximum);

          y -= drag;
          height += drag;
        }
        if (EAST == (direction & EAST)) {
          int drag = getDragDistance(current.x, pressed.x, snapSize.width);
          Dimension boundingSize = getBoundingSize(source);
          int maximum = Math.min(boundingSize.width - x, maximumSize.width);
          drag = getDragBounded(drag, snapSize.width, width, minimumSize.width, maximum);
          width += drag;
        }

        if (SOUTH == (direction & SOUTH)) {
          int drag = getDragDistance(current.y, pressed.y, snapSize.height);
          Dimension boundingSize = getBoundingSize(source);
          int maximum = Math.min(boundingSize.height - y, maximumSize.height);
          drag = getDragBounded(drag, snapSize.height, height, minimumSize.height, maximum);
          height += drag;
        }

        source.setBounds(x, y, width, height);
        source.validate();
      }

      private int getDragDistance(int larger, int smaller, int snapSize) {
        int halfway = snapSize / 2;
        int drag = larger - smaller;
        drag += (drag < 0) ? -halfway : halfway;
        drag = (drag / snapSize) * snapSize;

        return drag;
      }

      private int getDragBounded(int drag, int snapSize, int dimension, int minimum, int maximum) {
        while (dimension + drag < minimum)
          drag += snapSize;

        while (dimension + drag > maximum)
          drag -= snapSize;

        return drag;
      }

      private Dimension getBoundingSize(Component source) {
        if (source instanceof Window) {
          GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
          Rectangle bounds = env.getMaximumWindowBounds();
          return new Dimension(bounds.width, bounds.height);
        } else {
          return source.getParent().getSize();
        }
      }
    }

    private final JFrame frame;
    private JLabel status;
    private boolean maximizedFrame = false;
    private final int titleHeight;
    private int pX = 0;
    private int pY = 0;

    public TitledFrame(final TitleBarConfig conf, final int tH, final JComponent content, Runnable endExec) {
      this(conf, tH, content);
      frame.addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosing(WindowEvent e) {
          endExec.run();
        }

        @Override
        public void windowClosed(WindowEvent e) {
          endExec.run();
        }
      });
    }

    public static ComponentResizer cr = new ComponentResizer();

    public TitledFrame(final TitleBarConfig conf, final int tH, final JComponent content) {
      int titleHeightOffSub = 4;
      int contentOff = tH - titleHeightOffSub;

      frame = new JFrame();
      if (tH <= titleHeightOffSub || !Toolkit.getDefaultToolkit().isFrameStateSupported(Frame.MAXIMIZED_BOTH)) {
        frame.setUndecorated(false);
        this.titleHeight = 0;
      } else {
        frame.setUndecorated(true);
        this.titleHeight = tH;
      }

      /*------------------------------------------------------------ /
      / dont set the frame background to anything, causes flickering /
      /-------------------------------------------------------------*/

      frame.setTitle(conf.titleStr);
      if (conf.icon != null)
        frame.setIconImage(conf.icon.getImage());
      frame.setPreferredSize(
          new Dimension(content.getPreferredSize().width, titleHeight + content.getPreferredSize().height));
      frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
      if (content.getMaximumSize() == null
          || !(content.getMaximumSize().width == content.getPreferredSize().width
              && content.getMaximumSize().height == content.getPreferredSize().height)
          || (content.getMaximumSize().width <= frame.getMaximumSize().width
              && content.getMaximumSize().height <= frame.getMaximumSize().height)) {
        pstream.log.info("Registered A FRAME with component resizing abilities");
        cr.registerComponent(frame);
        frame.addMouseListener(cr);
      }
      /*----------------------------------------------------------------------------------------------------- /
      / need to set a border no matter what, or the component resizing routine cant be latched on graphically /
      /------------------------------------------------------------------------------------------------------*/
      frame.getRootPane()
          .setBorder(BorderFactory.createLineBorder(conf.borderColor != null ? conf.borderColor : Color.BLACK, 2));
      frame.setLocation(use_Program.screen_center().first - (frame.getPreferredSize().width / 2),
          use_Program.screen_center().second - (frame.getPreferredSize().height / 2));

      JPanel titleBar = new JPanel();
      titleBar.setPreferredSize(new Dimension(content.getPreferredSize().width, titleHeight));
      titleBar.addComponentListener(new ComponentAdapter() {
        @Override
        public void componentResized(ComponentEvent e) {
          if (titleBar.getPreferredSize().height > titleHeight) {
            titleBar.setSize(new Dimension(frame.getPreferredSize().width, titleHeight));
          }
        }
      });
      titleBar.addMouseListener(new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent me) {
          pX = me.getX();
          pY = me.getY();
        }

        @Override
        public void mouseDragged(MouseEvent me) {
          frame.setLocation(frame.getLocation().x + me.getX() - pX, frame.getLocation().y + me.getY() - pY);
        }
      });
      titleBar.addMouseMotionListener(new MouseMotionAdapter() {
        @Override
        public void mouseDragged(MouseEvent me) {
          frame.setLocation(frame.getLocation().x + me.getX() - pX, frame.getLocation().y + me.getY() - pY);
        }
      });
      titleBar.setOpaque(true);
      titleBar.setBackground(conf.bg);
      titleBar.setLayout(new BorderLayout(10, 0));

      JLabel titleBarStr = new JLabel(conf.titleStr);
      if (conf.titleStrFont != null)
        titleBarStr.setFont(conf.titleStrFont);
      titleBarStr.setForeground(conf.fg);
      titleBarStr.setHorizontalAlignment(SwingConstants.CENTER);
      titleBarStr.setVerticalAlignment(SwingConstants.CENTER);

      JPanel btns = new JPanel();
      btns.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 0));
      btns.setOpaque(true);
      btns.setBackground(conf.bg);

      status = new JLabel("", SwingConstants.CENTER);
      status.setPreferredSize(new Dimension(const_Manager.FRAME_TITLEBAR_HEIGHT, const_Manager.FRAME_TITLEBAR_HEIGHT));
      status.setMaximumSize(new Dimension(const_Manager.FRAME_TITLEBAR_HEIGHT, const_Manager.FRAME_TITLEBAR_HEIGHT));
      status.setOpaque(true);
      status.setAutoscrolls(true);
      btns.add(status);

      if (conf.bgMis != null)
        btns.add(gen_Button(conf.bgMis, () -> {
        }));
      if (conf.bgExp != null) {
        btns.add(gen_Button(conf.bgExp, () -> {
          frame.setExtendedState(maximizedFrame ? Frame.NORMAL : Frame.MAXIMIZED_BOTH);
          maximizedFrame = !maximizedFrame;
        }));
      }
      if (conf.bgMini != null) {
        btns.add(gen_Button(conf.bgMini, () -> frame.setState(Frame.ICONIFIED)));
      }
      btns.add(gen_Button(conf.bgClose, frame::dispose));

      JLabel titleBarICO = new JLabel(conf.icon != null
          ? new ImageIcon(conf.icon.getImage().getScaledInstance(contentOff, contentOff, Image.SCALE_AREA_AVERAGING))
          : new ImageIcon());
      if (conf.icon == null) {
        titleBarICO.setPreferredSize(new Dimension(titleHeight, titleHeight));
        titleBarICO.setOpaque(true);
        titleBarICO.setBackground(conf.fg);
      }
      titleBarICO.setVerticalAlignment(SwingConstants.CENTER);

      titleBar.add(titleBarICO, BorderLayout.WEST);
      titleBar.add(titleBarStr, BorderLayout.CENTER);
      titleBar.add(btns, BorderLayout.EAST);

      JPanel bigPane = new JPanel();
      bigPane.setPreferredSize(frame.getPreferredSize());
      bigPane.setLayout(new BorderLayout());

      bigPane.add(titleBar, BorderLayout.NORTH);
      bigPane.add(content, BorderLayout.SOUTH);

      frame.getContentPane().add(bigPane);
    }

    public void askStatus(struct_Trio<ImageIcon, String, Optional<Runnable>> exec, boolean autoresize) {
      pstream.log.info("Status Update for frame title: " + exec.second);
      use_Task.run_Snb_1(() -> {
        status.setToolTipText(exec.second);
        if (autoresize) {
          exec.first.setImage(use_Image
              .resize_fast_1(status.getPreferredSize().width, status.getPreferredSize().height, exec.first).getImage());
        }
        status.setIcon(exec.first);
        pstream.log.info("Status Update for frame icon: " + exec.first.getDescription());
        status.repaint(50L);
        status.revalidate();
        exec.third.ifPresent(Runnable::run);
      });
    }

    public void askStatus() {
      pstream.log.info("Status cancelling...");
      use_Task.run_Snb_1(() -> {
        status.setToolTipText(null);
        status.setIcon(null);
      });
    }

    public JFrame expose() {
      return frame;
    }

    private static class TitleBarButton extends JButton {
      private boolean hovering = false;
      private final int size;
      private final Color clr;

      public TitleBarButton(int size, Color color, Runnable func) {
        this.size = size;
        this.clr = color;

        addMouseListener(new MouseAdapter() {
          @Override
          public void mousePressed(MouseEvent e) {
            hovering = true;
            SwingUtilities.invokeLater(() -> repaint(50L));
          }

          @Override
          public void mouseExited(MouseEvent e) {
            hovering = false;
            SwingUtilities.invokeLater(() -> repaint(50L));
          }

          @Override
          public void mouseEntered(MouseEvent e) {
            hovering = true;
            SwingUtilities.invokeLater(() -> repaint(50L));
          }

          @Override
          public void mouseClicked(MouseEvent e) {
            hovering = true;
            SwingUtilities.invokeLater(() -> repaint(50L));
          }
        });

        addActionListener(x -> func.run());
        setRolloverEnabled(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setPreferredSize(new Dimension(size + 1, size + 1));
        setMaximumSize(new Dimension(size + 1, size + 1));
      }

      @Override
      public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(clr);
        g2.fillOval(0, 0, size, size);
        if (hovering && this.clr != null) {
          g2.setColor(clr.darker().darker());
          // ON LOW RES SCREENS, MUST BE ODD NUMBER TO WORK PROPERLY WITH
          int SMALL_RADIUS = 7;
          g2.fillOval((size / 2) - (SMALL_RADIUS / 2), (size / 2) - (SMALL_RADIUS / 2), SMALL_RADIUS, SMALL_RADIUS);
        }
        g2.dispose();
        g.dispose();
      }

    }

    private static JButton gen_Button(Color color, Runnable func) {
      return new TitleBarButton(13, color, func);
    }

    @Override
    public void run() {
      /*--------------------------------------------------------------------------------------- /
      / AHHH there's some funky shit with why it sometimes renders the frame with Transparency. /
      /----------------------------------------------------------------------------------------*/

      frame.pack();
      frame.setVisible(true);
    }
  }

  public static class TitleBarConfig {
    public String titleStr;
    public ImageIcon icon;
    public Color fg, bg, bgClose, bgMini, bgExp, bgMis, borderColor;
    public Font titleStrFont;

    // required parameters: 8
    public TitleBarConfig(String str, ImageIcon ico, Font titleStrFont, Color fg, Color bg, Color bgClose, Color bgMini,
        Color bgExp, Color bgMis, Color borderColor) {
      this.titleStr = str;
      this.icon = ico;
      this.fg = fg;
      this.bg = bg;
      this.bgClose = bgClose;
      this.bgMini = bgMini;
      this.bgExp = bgExp;
      this.bgMis = bgMis;
      this.titleStrFont = titleStrFont;
      this.borderColor = borderColor;
    }
  }

  private final TitledFrame frame;

  public gui_HalcyonFrame(JComponent top, JComponent bottom) {
    if (const_Manager.DEBUG_GRAPHICS) {
      top.setOpaque(true);
      top.setBackground(use_Color.rndColor());
      bottom.setOpaque(true);
      bottom.setBackground(use_Color.rndColor());
    }

    JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, bottom);
    splitPane.setDividerLocation(const_Manager.FRAME_MIN_WIDTH / 2);
    splitPane.setDividerSize(0);
    splitPane.setPreferredSize(new Dimension(const_Manager.FRAME_MIN_WIDTH, const_Manager.FRAME_MIN_HEIGHT));

    frame = new TitledFrame(new TitleBarConfig("Halcyon",
        use_ResourceFetcher.fetcher.getFromAsImageIcon(const_ResourceManager.GUI_PROGRAM_LOGO),
        use_HalcyonProperties.regularFont().deriveFont(const_Manager.PROGRAM_DEFAULT_FONT_SIZE),
        const_ColorManager.DEFAULT_GREEN_FG, const_ColorManager.DEFAULT_BG, const_ColorManager.DEFAULT_RED_FG,
        const_ColorManager.DEFAULT_YELLOW_FG, const_ColorManager.DEFAULT_GREEN_FG, const_ColorManager.DEFAULT_PINK_FG,
        const_ColorManager.DEFAULT_BG),
        const_Manager.FRAME_TITLEBAR_HEIGHT, splitPane);

    frame.expose().addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosed(WindowEvent e) {
        /*------------------------------------------------------------------------------ /
        / dont have to wait for the JVM to actually realise the root component is gone!! /
        /-------------------------------------------------------------------------------*/
        System.exit(0);
      }
    });
    frame.expose().getRootPane().setBorder(BorderFactory.createLineBorder(const_ColorManager.DEFAULT_BG, 4));
  }

  /**
   * @return JFrame
   */
  public final JFrame expose() {
    return frame.expose();
  }

  public final TitledFrame expose_internal() {
    return frame;
  }

  @Override
  public final void run() {
    frame.run();
  }
}
