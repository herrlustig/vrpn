
package vrpn;
import java.util.*;

public class AnalogRemote extends TimerTask
{
	
	//////////////////
	// Public structures and interfaces
	
	public static final int MAX_CHANNELS = 128;
	
	public class AnalogUpdate
	{
		public Date msg_time = new Date( );
		public double channel[];
	}
	
	public interface AnalogChangeListener
	{
		public void analogUpdate( AnalogUpdate a, AnalogRemote analog );
	}

	// end of the public structures and interfaces
	///////////////////
	
	///////////////////
	// Public methods
	
	/**
	 * @exception java.lang.InstantiationException
	 *		If the analog could not be created because of problems with
	 *      its native code and linking.
	 */
	public AnalogRemote( String name ) throws InstantiationException
	{
		try { this.init( name ); }
		catch( java.lang.UnsatisfiedLinkError e )
		{  
			System.out.println( "Error initializing remote analog device " + name + "." );
			System.out.println( " -- Unable to find the right functions.  This may be a version problem." );
			throw new InstantiationException( e.getMessage( ) );
		}
		
		analogTimer.scheduleAtFixedRate( this, 0, timerPeriod );
	}
	
	public synchronized native boolean requestValueChange( int channel, double value );
	
	public synchronized void addAnalogChangeListener( AnalogChangeListener listener )
	{
		changeListeners.addElement( listener );
	}
	
	public synchronized boolean removeAnalogChangeListener( AnalogChangeListener listener )
	{
		return changeListeners.removeElement( listener );
	}
	
	/**
	 * This should <b>not</b> be called by user code.
	 */
	public void run( )
	{
		this.mainloop( );
	}
	
	// end public methods
	////////////////////////
	
	
	////////////////////////
	// Protected methods
	//
	
	/**
	 * Should be called only by mainloop(), a native method which is itself
	 * synchronized.  By extension, this method is synchronized (in that, for
	 * a given AnalogRemote object, this method can only be called from one
	 * thread at a time).
	 */
	protected void handleAnalogChange( long tv_sec, long tv_usec, 
									   double[] channel )
	{
		// putting the body of this function into a synchronized block prevents
		// other instances of AnalogRemote from calling listeners' analogUpdate
		// method concurrently.
		synchronized( notifyingChangeListenersLock )
		{
			AnalogUpdate a = new AnalogUpdate();
			a.msg_time.setTime( tv_sec * 1000 + tv_usec );
			a.channel = (double[]) channel.clone( );
			
			// notify all listeners
			Enumeration e = changeListeners.elements( );
			while( e.hasMoreElements( ) )
			{
				AnalogChangeListener l = (AnalogChangeListener) e.nextElement( );
				l.analogUpdate( a, this );
			}
		} // end synchronized( notifyingChangeListenersLock )
	} // end method handleAnalogChange
	
	
	/**
	 * Initialize the native analog object
	 * @param name The name of the analog and host (e.g., <code>"Analog0@myhost.edu"</code>).
	 * @return <code>true</code> if the analog was connected successfully, 
	 *			<code>false</code> otherwise.
	 */
	protected native boolean init( String name );

	/**
	 * This should only be called from the method finalize()
	 */
	protected native void shutdownAnalog( );
	
	protected synchronized native void mainloop( );
	
	protected void finalize( ) throws Throwable
	{
		this.cancel( );
		analogTimer.cancel( );
		changeListeners.removeAllElements( );
		this.shutdownAnalog( );
	}
	
	
	// end protected methods
	///////////////////////
	
	///////////////////
	// data members
	
	// this is used by the native code to store a C++ pointer to the 
	// native vrpn_AnalogRemote object
	protected int native_analog = -1;
	
	// the Timer
	protected Timer analogTimer = new Timer( );
	protected long timerPeriod = 100; // milliseconds
	
	/**
	 * @see vrpn.AnalogRemote.notifyingChangeListenersLock
	 */
	protected final static Object notifyingChangeListenersLock = new Object( );
	protected Vector changeListeners = new Vector( );


	// static initialization
	static 
	{
		try { System.loadLibrary( "AnalogRemote" ); }
		catch( UnsatisfiedLinkError e )
		{
			System.out.println( e.getMessage( ) );
			System.out.println( "Error initializing remote analog device." );
			System.out.println( " -- Unable to find native library." );
		}
		catch( SecurityException e )
		{
			System.out.println( e.getMessage( ) );
			System.out.println( "Security exception:  you couldn't load the native analog remote dll." );
		}
	}
	
	

}
